/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.tracing;

import com.example.Main;
import com.example.wiring.pubsub.DockerIntegrationTest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = Main.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("docker-it-test")
public class TracingIntegrationTest extends DockerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(TracingIntegrationTest.class);
    static Config config = ConfigFactory.parseString("""
                kalix.telemetry.tracing.collector-endpoint = "http://localhost:4317"
                """);

    public TracingIntegrationTest(ApplicationContext applicationContext) {
        super(applicationContext, config);
    }

    @Test //disabled ATM for https://github.com/lightbend/kalix-jvm-sdk/pull/1810
    public void shouldSendTraces() {
        String counterId = "some-counter";
        callTCounter(counterId, 10);

        await().ignoreExceptions().atMost(20, TimeUnit.of(SECONDS)).untilAsserted(() -> {
           Traces traces = selectTraces();
           logger.debug("Traces found: {}", traces);
           assertThat(traces.traces().isEmpty()).isFalse();
           Batches batches = selectBatches(traces.traces().get(0).traceID());
           assertThat(batches.batches().isEmpty()).isFalse();
           logger.debug("Batches found: [{}]", batches.batches());
           //Name of the tracer when the span is built. See `Telemetry.buildSpan`
           var javaSdkBatches =  Batches.findBatchesWithScopeName(batches, "java-sdk");
           assertThat(Batches.findSpansWithName(javaSdkBatches, "Increase")).isNotEmpty();


           var javaSdkBatches2 =  Batches.findBatchesWithScopeName(batches, "kalix");
           assertThat(Batches.findSpansWithName(javaSdkBatches2,"PrintIncrease")).isNotEmpty();
        }
        );

    }

    private Integer callTCounter(String counterId, Integer increase) {
        return webClient.post().uri("/tcounter/" + counterId + "/increase/" + increase).retrieve().bodyToMono(Integer.class).block(timeout);
    }
    public Traces selectTraces(){
        Traces traces = WebClient.create("http://0.0.0.0:3200/api/search").get().retrieve().bodyToMono(Traces.class).block(timeout);
        return traces;
    }

    public Batches selectBatches(String traceId){
        Batches batches =  WebClient.create("http://0.0.0.0:3200/api/traces/" + traceId).get().retrieve().bodyToMono(Batches.class).block(timeout);
        return batches;
    }

}

