package com.example;

import com.example.actions.CounterCommandFromTopicAction;
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
@Import(TestKitConfiguration.class)
@ActiveProfiles("with-pubsub")
public class CounterIntegrationWithRealPubSubTest extends KalixIntegrationTestKitSupport { // <1>

// end::class[]

    private Duration timeout = Duration.of(10, SECONDS);

    @Autowired
    private WebClient webClient;

    @Test
    public void verifyCounterEventSourcedConsumesFromPubSub() {
        WebClient pubsubClient = WebClient.builder()
            .baseUrl("http://localhost:8085")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        var counterId = "testRealPubSub";
        var messageBody = buildMessageBody(
            "{\"counterId\":\"" + counterId + "\",\"value\":20}",
            CounterCommandFromTopicAction.IncreaseCounter.class.getName());

        var projectId = "test";
        var injectMsgResult = pubsubClient.post()
            .uri("/v1/projects/{projectId}/topics/counter-commands:publish", projectId)
            .bodyValue(messageBody)
            .retrieve()
            .toBodilessEntity().block();
        assertTrue(injectMsgResult.getStatusCode().is2xxSuccessful());

        await()
            .ignoreExceptions()
            .atMost(20, TimeUnit.SECONDS)
            .until(() ->
                    webClient.get()
                        .uri("/counter/" + counterId)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(timeout),
                new IsEqual("\"20\"")
            );
    }
    // end::test-topic[]

    // builds a message in PubSub format, ready to be injected
    private String buildMessageBody(String jsonMsg, String ceType) {
        var data = Base64.getEncoder().encodeToString(jsonMsg.getBytes());

        return """
            {
                "messages": [
                    {
                        "data": "%s",
                        "attributes": {
                            "Content-Type": "application/json",
                            "ce-specversion": "1.0",
                            "ce-type": "%s"
                        }
                    }
                ]
            }
            """.formatted(data, ceType);
    }

// tag::class[]
}
// end::class[]
