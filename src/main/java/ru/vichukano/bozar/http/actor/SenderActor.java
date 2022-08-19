package ru.vichukano.bozar.http.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.Getter;
import lombok.Value;
import ru.vichukano.bozar.http.actor.DispatcherActor.DispatcherMessage;
import ru.vichukano.bozar.http.actor.DispatcherActor.SenderAnswer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SenderActor extends AbstractActor<SenderActor.SenderMessage> {
    private static final Set<Integer> SUCCESS_CODES = IntStream.rangeClosed(200, 299)
        .boxed().collect(Collectors.toSet());
  @Getter
  private final String name;

  private SenderActor(ActorContext<SenderActor.SenderMessage> context, String name) {
    super(context);
    this.name = name;
    log().debug("Create actor with name: {}", name);
  }

  public static Behavior<SenderMessage> create(String name) {
    return Behaviors.setup(ctx -> new SenderActor(ctx, name));
  }

  public interface SenderMessage {
  }

  public static final class Kill implements SenderMessage {
  }

  @Value
  public static class Send implements SenderMessage {
    String addres;
    String messageToSend;
    TimeoutInfo timeoutInfo;
    ActorRef<DispatcherMessage> replyTo;

    @Value
    public static class TimeoutInfo {
      Duration connectionTimeout;
      Duration responseTimeout;
    }
  }

  @Override
  public Receive<SenderActor.SenderMessage> createReceive() {
    return newReceiveBuilder().onMessage(Kill.class, m -> Behaviors.stopped())
        .onMessage(Send.class, m -> onSendMessageReceive(m)).build();
  }

  private Behavior<SenderMessage> onSendMessageReceive(Send sendMessage) {
    log().debug("Receive send command: {}", sendMessage);
    final String address = sendMessage.getAddres();
    final String messagePayload = sendMessage.getMessageToSend();
    log().trace("Start to send POST to addres: {} message {}", address, messagePayload);
    try {
      final HttpRequest request = HttpRequest.newBuilder().uri(new URI(address))
          .POST(BodyPublishers.ofString(messagePayload))
          .header("Content-Type", "application/json")
          .timeout(sendMessage.getTimeoutInfo().getResponseTimeout()).build();
      final HttpClient client = HttpClient.newBuilder()
          .connectTimeout(sendMessage.getTimeoutInfo().getConnectionTimeout()).build();
      var now = LocalDateTime.now();
      final HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString());
      var after = LocalDateTime.now();
      log().debug("Response message: {}", response.body());
      if (!SUCCESS_CODES.contains(response.statusCode())) {
        throw new IllegalStateException("Wrong status code: " + response.statusCode());
      }
      sendMessage.getReplyTo().tell(new SenderAnswer(name, true, null, Duration.between(now, after)));
    } catch (Exception e) {
      log().debug("Failed to send message, cause: {}", e.getMessage(), e);
      sendMessage.getReplyTo().tell(new SenderAnswer(name, false, e, Duration.ZERO));
    }
    return this;
  }

}
