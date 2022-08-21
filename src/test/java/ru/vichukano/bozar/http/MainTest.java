package ru.vichukano.bozar.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class MainTest {
  private static final WireMockServer SERVER =
      new WireMockServer(new WireMockConfiguration().port(8082));

  @BeforeAll
  static void setUp() {
    configureFor("localhost", 8082);
    SERVER.start();
  }

  @AfterAll
  static void reset() {
    SERVER.stop();
  }

  @Test
  @DisplayName("Throw exception for wrong argumenst")
  void shouldThrowExceptionIfWrongArguments() {
    Assertions.assertThatThrownBy(() -> Main.main(new String[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldSendHttpRequest() {
    stubFor(post(urlEqualTo("/test")).willReturn(aResponse().withBody("World")));
    final String urlArg = "http://localhost:8082/test";
    final String clientsArgs = "100";
    final String timesArgs = "100";
    final String connTimeoutSec = "3";
    final String respTimeoutSec = "4";
    final String messageArg = "Hello";
    final String[] args = {urlArg, clientsArgs, timesArgs, connTimeoutSec , respTimeoutSec, messageArg};

    Main.main(args);
  }
}

