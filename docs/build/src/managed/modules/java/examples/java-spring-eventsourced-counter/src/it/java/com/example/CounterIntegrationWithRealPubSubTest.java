package com.example;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// tag::class[]
@SpringBootTest(classes = Main.class)
@Import(TestkitConfig.class)
@ActiveProfiles("with-pubsub")
public class CounterIntegrationWithRealPubSubTest extends KalixIntegrationTestKitSupport { // <1>

// end::class[]

    private Duration timeout = Duration.of(10, SECONDS);

    @Autowired
    private WebClient webClient;

    // tag::test-topic[]
    @Autowired
    private KalixTestKit kalixTestKit; // <2>

    // tag::test-topic[]

    @Test
    public void verifyCounterEventSourcedConsumesFromPubSub() {

        var projectId = "test";

        WebClient pubsubClient = WebClient.builder()
            .baseUrl("http://localhost:8085") // Replace with your Pub/Sub emulator URL
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        var jsonMsg = "{\"counterId\":\"oi\",\"value\":20}";
        var data = Base64.getEncoder().encodeToString(jsonMsg.getBytes());

        var messageBody =
            "{\n" +
            "    \"messages\": [\n" +
            "        {\n" +
            "            \"data\": \"" + data + "\",\n" +
            "            \"attributes\": {\n" +
            "                \"Content-Type\": \"application/json\",\n" +
            "                \"ce-specversion\": \"1.0\",\n" +
            "                \"ce-type\": \"com.example.actions.CounterCommandFromTopicAction$IncreaseCounter\"\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

        var injectResult = pubsubClient.post()
            .uri("/v1/projects/{projectId}/topics/counter-commands:publish", projectId)
            .bodyValue(messageBody)
            .retrieve()
            .toBodilessEntity().block();
        assertTrue(injectResult.getStatusCode().is2xxSuccessful());

        await()
            .ignoreExceptions()
            .atMost(20, TimeUnit.SECONDS)
            .until(() ->
                    webClient.get()
                        .uri("/counter/oi")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(timeout),
                new IsEqual("\"20\"")
            );
    }
    // end::test-topic[]

// tag::class[]
}
// end::class[]
