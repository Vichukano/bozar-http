package ru.vichukano.bozar.http;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
    final int clients = Integer.parseInt(args[1]);
    final int times = Integer.parseInt(args[2]);
    final Duration conTimeout = Duration.ofSeconds(Long.parseLong(args[3]));
    final Duration respTimeout = Duration.ofSeconds(Long.parseLong(args[4]));
    final String fileContent = args[5];
    log.info("Path: {}, clients: {}, times: {}, connectionTimeout: {}, responseTimeout: {}, fileContent: {}",
        path, clients, times, conTimeout, respTimeout, fileContent);
    final var flag = new AtomicBoolean(false);
    final ActorSystem<DispatcherActor.DispatcherMessage> dispatcher =
        ActorSystem.create(DispatcherActor.create(path, flag, new AtomicLong((long) clients * times)), "sender-dispatcher");
    dispatcher.tell(new DispatcherActor.StartDispatcher(clients, fileContent, conTimeout, respTimeout));
    var now = LocalDateTime.now();
    while (!flag.get()) {
    }
    var after = LocalDateTime.now();
    log.info("**************************Finish**************************");
    log.info("Time spend: {} ms", Duration.between(now, after).toMillis());
  }

}
