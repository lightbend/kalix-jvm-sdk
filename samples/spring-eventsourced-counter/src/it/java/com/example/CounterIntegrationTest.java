package com.example;

import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
// tag::class[]
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@Import(TestConfig.class)  // <1>
public class CounterIntegrationTest extends KalixIntegrationTestKitSupport {
// end::class[]


    @Autowired
    private WebClient webClient;

    private Duration timeout = Duration.of(10, SECONDS);

    @Test
    public void verifyCounterEventSourcedWiring() {

        String counterIncrease =
                webClient
                        .post()
                        .uri("/counter/hello/increase/10")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(timeout);

        Assertions.assertEquals("\"10\"", counterIncrease);

        String counterMultiply =
                webClient
                        .post()
                        .uri("/counter/hello/multiply/20")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(timeout);

        Assertions.assertEquals("\"200\"", counterMultiply);

        String counterGet =
                webClient.get().uri("/counter/hello").retrieve().bodyToMono(String.class).block(timeout);

        Assertions.assertEquals("\"200\"", counterGet);
    }

    @Test
    public void forbiddenCall(){
         webClient
                .post()
                .uri("/counter/id1/forbiddenIncrease/1")
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout);
    }
// tag::class[]
}
// end::class[]