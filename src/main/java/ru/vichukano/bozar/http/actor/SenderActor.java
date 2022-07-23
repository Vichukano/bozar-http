package ru.vichukano.bozar.http.actor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.Getter;
import lombok.Value;
import ru.vichukano.bozar.http.actor.DispatcherActor.DispatcherMessage;
import ru.vichukano.bozar.http.actor.DispatcherActor.SenderAnswer;

public class SenderActor extends AbstractActor<SenderActor.SenderMessage> {
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
          .timeout(sendMessage.getTimeoutInfo().getResponseTimeout()).build();
      final HttpClient client = HttpClient.newBuilder()
          .connectTimeout(sendMessage.getTimeoutInfo().getConnectionTimeout()).build();
      final HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString());
      log().debug("Response message: {}", response.body());
      sendMessage.getReplyTo().tell(new SenderAnswer(name, true, null));
    } catch (Exception e) {
      log().debug("Failed to send message, cause: {}", e);
      sendMessage.getReplyTo().tell(new SenderAnswer(name, false, e));
    }
    return this;
  }

}

