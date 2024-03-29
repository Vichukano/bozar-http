package ru.vichukano.bozar.http.actor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.Value;

public class DispatcherActor extends AbstractActor<DispatcherActor.DispatcherMessage> {
  private final Map<Long, String> failReasons = new TreeMap<>();
  private final Map<Long, String> successReport = new TreeMap<>();
  private final List<Long> successTimeouts = new LinkedList<>();
  private final AtomicLong succesSendCounter = new AtomicLong();
  private final AtomicLong failedSendCounter = new AtomicLong();
  private final AtomicLong allCount = new AtomicLong();
  private final AtomicBoolean stopFlag;
  private final AtomicLong requests;
  private final Map<String, ActorRef<SenderActor.SenderMessage>> senderActors = new HashMap<>();
  private final String addres;

  private DispatcherActor(ActorContext<DispatcherMessage> context, String addres,
      AtomicBoolean stopFlag, AtomicLong requests) {
    super(context);
    this.addres = addres;
    this.stopFlag = stopFlag;
    this.requests = requests;
  }

  public static Behavior<DispatcherMessage> create(String addres, AtomicBoolean stopFlag, AtomicLong requests) {
    return Behaviors.setup(ctx -> new DispatcherActor(ctx, addres, stopFlag, requests));
  }

  public interface DispatcherMessage {
  }

  @Value
  public static class SenderAnswer implements DispatcherMessage {
    String senderName;
    boolean success;
    Throwable error;
    Duration duration;
  }

  @Value
  public static class StartDispatcher implements DispatcherMessage {
    int clients;
    String message;
    Duration connectionTimeout;
    Duration responseTimeout;
  }

  @Override
  public Receive<DispatcherMessage> createReceive() {
    return newReceiveBuilder().onMessage(SenderAnswer.class, m -> onSenderAnswer(m))
        .onMessage(StartDispatcher.class, m -> onStartReceive(m)).build();
  }

  private Behavior<DispatcherMessage> onSenderAnswer(SenderAnswer answer) {
    log().trace("Receive answer: {}", answer);
    requests.decrementAndGet();
    if (answer.isSuccess()) {
      log().trace("Success answer");
      succesSendCounter.incrementAndGet();
      long all = allCount.incrementAndGet();
      long durationMs = answer.duration.toMillis();
      successReport.put(all, "SUCCESS: " + all + " request duration: " + durationMs + "ms");
      successTimeouts.add(durationMs);
    } else {
      log().trace("Failed answer, cause: {}", answer.getError().getMessage());
      failedSendCounter.incrementAndGet();
      long all = allCount.incrementAndGet();
      failReasons.put(all, "FAILED: " + all + " reason: " + answer.error.getMessage());
    }
    if (requests.get() == 0L) {
      log().info("Statistic: success: {}, failed: {}", succesSendCounter.get(), failedSendCounter.get());
      successReport.putAll(failReasons);
      String report = String.join("\n", new ArrayList<>(this.successReport.values()));
      OptionalDouble average = this.successTimeouts.stream().mapToLong(Long::longValue).average();
      log().info("\n{}", report);
      log().info("Average response time: {} ms", average);
      this.stopFlag.set(true);
      senderActors.forEach((k, v) -> v.tell(new SenderActor.Kill()));
      return Behaviors.stopped();
    }
    return this;
  }

  private Behavior<DispatcherMessage> onStartReceive(StartDispatcher start) {
    log().info("Receive start command: {}", start);
    for (long i = 0; i < start.getClients(); i++) {
      final var name = "sender-" + i;
      final var ref = getContext().spawn(SenderActor.create(name), name);
      this.senderActors.put(name, ref);
      log().debug("Spawn new actor with name: {}", name);
    }
    final var ti = new SenderActor.Send.TimeoutInfo(start.connectionTimeout, start.responseTimeout);
    for (long i = 0; i < requests.get(); i++) {
      senderActors.forEach((k, v) -> v.tell(new SenderActor.Send(addres, start.getMessage(), ti, getContext().getSelf())));
    }
    log().debug("Send {} messages to actors: {}", start.getClients(), senderActors);
    return this;
  }

}

