package com.example;

import com.example.actions.CounterCommandFromTopicAction;
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

    private EventingTestKit.Topic commandsTopic;
    private EventingTestKit.Topic eventsTopic;

    @BeforeAll
    public void beforeAll() {
        commandsTopic = kalixTestKit.getTopic("counter-commands");
        eventsTopic = kalixTestKit.getTopic("counter-events");
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

        var counterId = "pubsub-test";
        var increaseCmd1 = new CounterCommandFromTopicAction.IncreaseCounter(counterId, 3);
        var increaseCmd2 = new CounterCommandFromTopicAction.IncreaseCounter(counterId, 4);

        commandsTopic.publish(increaseCmd1, counterId);
        commandsTopic.publish(increaseCmd2, counterId);

        var eventIncreased1 = eventsTopic.expectOneTyped(CounterEvent.ValueIncreased.class);
        assertEquals(new CounterEvent.ValueIncreased(increaseCmd1.value()), eventIncreased1.getPayload());

        var eventIncreased2 = eventsTopic.expectOneTyped(CounterEvent.ValueIncreased.class);
        assertEquals(new CounterEvent.ValueIncreased(increaseCmd2.value()), eventIncreased2.getPayload());
    }
// tag::class[]
}
// end::class[]