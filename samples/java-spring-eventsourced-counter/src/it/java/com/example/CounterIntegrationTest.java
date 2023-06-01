package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

// tag::class[]
@SpringBootTest(classes = Main.class)
@Import(TestkitConfig.class)  // <1>
public class CounterIntegrationTest extends KalixIntegrationTestKitSupport {
// end::class[]


    @Autowired
    private WebClient webClient;

    @Autowired
    private KalixTestKit kalixTestKit;

    private Duration timeout = Duration.of(10, SECONDS);

    private EventingTestKit.Topic outTopic;

    @BeforeAll
    public void beforeAll() {
        outTopic = kalixTestKit.getTopic("counter-events");
    }


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
    public void verifyCounterEventSourcedPublishToTopic() throws JsonProcessingException {

        String counterIncrease =
            webClient
                .post()
                .uri("/counter/to-topic/increase/10")
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout);
        assertEquals("\"10\"", counterIncrease);

        String counterMultiply =
            webClient
                .post()
                .uri("/counter/to-topic/multiply/20")
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout);
        assertEquals("\"200\"", counterMultiply);

        var eventIncreased = outTopic.expectOneTyped(CounterEvent.ValueIncreased.class);
        assertEquals(new CounterEvent.ValueIncreased(10), eventIncreased.getPayload());

        var eventMultiply = outTopic.expectOneTyped(CounterEvent.ValueMultiplied.class);
        assertEquals(new CounterEvent.ValueMultiplied(20), eventMultiply.getPayload());
    }
// tag::class[]
}
// end::class[]