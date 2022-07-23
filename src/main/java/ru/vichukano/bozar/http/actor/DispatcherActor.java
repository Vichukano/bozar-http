package ru.vichukano.bozar.http.actor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.Value;

public class DispatcherActor extends AbstractActor<DispatcherActor.DispatcherMessage> {
  private final AtomicLong succesSendCounter = new AtomicLong();
  private final AtomicLong failedSendCounter = new AtomicLong();
  private final AtomicBoolean stopFlag;
  private final Map<String, ActorRef<SenderActor.SenderMessage>> senderActors = new HashMap<>();
  private final String addres;

  private DispatcherActor(ActorContext<DispatcherMessage> context, String addres,
      AtomicBoolean stopFlag) {
    super(context);
    this.addres = addres;
    this.stopFlag = stopFlag;
  }

  public static Behavior<DispatcherMessage> create(String addres, AtomicBoolean stopFlag) {
    return Behaviors.setup(ctx -> new DispatcherActor(ctx, addres, stopFlag));
  }

  public interface DispatcherMessage {
  }

  @Value
  public static class SenderAnswer implements DispatcherMessage {
    String senderName;
    boolean success;
    Throwable error;
  }

  @Value
  public static class StartDispatcher implements DispatcherMessage {
    long clients;
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
    if (answer.isSuccess()) {
      log().trace("Success answer");
      succesSendCounter.incrementAndGet();
    } else {
      log().trace("Failed answer, cause: {}", answer.getError().getMessage());
      failedSendCounter.incrementAndGet();
    }
    if (!senderActors.isEmpty()) {
      senderActors.remove(answer.getSenderName()).tell(new SenderActor.Kill());
    }
    if (senderActors.isEmpty()) {
      log().info("Statistic: success: {}, failed: {}", succesSendCounter.get(),
          failedSendCounter.get());
      this.stopFlag.set(true);
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
    final var ti =
        new SenderActor.Send.TimeoutInfo(start.getConnectionTimeout(), start.getResponseTimeout());
    senderActors.forEach((k, v) -> v
        .tell(new SenderActor.Send(addres, start.getMessage(), ti, getContext().getSelf())));
    log().debug("Send {} messages to actors: {}", start.getClients(), senderActors);
    return this;
  }

}

