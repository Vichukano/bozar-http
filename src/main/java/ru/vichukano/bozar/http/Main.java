package ru.vichukano.bozar.http;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import akka.actor.typed.ActorSystem;
import lombok.extern.slf4j.Slf4j;
import ru.vichukano.bozar.http.actor.DispatcherActor;

@Slf4j
public class Main {

  public static void main(String[] args) {
    log.info("Start Bozar with args: {}", Arrays.toString(args));
    if (args.length < 3) {
      throw new IllegalArgumentException("Input path to send, numbers of clients and json file");
    }
    final String path = args[0];
    final long clients = Long.parseLong(args[1]);
    final Duration conTimeout = Duration.ofSeconds(Long.parseLong(args[2]));
    final Duration respTimeout = Duration.ofSeconds(Long.parseLong(args[3]));
    final String fileContent = args[4];
    log.info("Paht: {}, clients: {}, connectionTimeout: {}, responseTimeout: {}, fileContent: {}",
        path, clients, conTimeout, respTimeout, fileContent);
    final var flag = new AtomicBoolean(false);
    final ActorSystem<DispatcherActor.DispatcherMessage> dispatcher =
        ActorSystem.create(DispatcherActor.create(path, flag), "sender-dispatcher");
    dispatcher
        .tell(new DispatcherActor.StartDispatcher(clients, fileContent, conTimeout, respTimeout));
    while (!flag.get()) {
    }
    log.info("**************************Finish**************************");
  }

}
