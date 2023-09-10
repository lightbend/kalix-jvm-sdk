package com.example.wiring.tracing;

import com.example.Main;
import com.example.wiring.pubsub.DockerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = Main.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("docker-it-test")
public class TracingIntegratonTest extends DockerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(TracingIntegratonTest.class);

    public TracingIntegratonTest(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Test
    public void shouldSendTraces() {
        String counterId = "some-counter";
        callTCounter(counterId, 10);
        Traces traces = selectTraces();
        Batches batches = selectBatches(traces.traces().get(0).traceID());

        await().ignoreExceptions().atMost(20, TimeUnit.of(SECONDS)).untilAsserted(() -> {
           assertThat(batches.batches().get(0).scopeSpans().get(0).scope().name()).isEqualTo("kalix.proxy.telemetry.TraceInstrumentationImpl");
           assertThat(batches.batches().get(1).scopeSpans().get(0).spans().get(0).name()).isEqualTo("com.example.wiring.eventsourcedentities.tracingcounter.TCounterEntity.increase");
           assertThat(batches.batches().get(2).scopeSpans().get(0).spans().get(0).name()).isEqualTo("com.example.wiring.eventsourcedentities.tracingcounter.TIncreaseAction.printIncrease");
        }
        );

    }

    private Integer callTCounter(String counterId, Integer increase) {
        return webClient.post().uri("/tcounter/" + counterId + "/increase/" + increase).retrieve().bodyToMono(Integer.class).block();
    }
 // TODO investigate this path   String response = WebClient.create("http://0.0.0.0:3200").get().uri(
 //                uriBuilder -> uriBuilder.path("/api/search")
 //                        .queryParam("q","{ }").build()).retrieve().bodyToMono(String.class).block(timeout)
    public Traces selectTraces(){
      ;
        Traces traces = WebClient.create("http://0.0.0.0:3200/api/search").get().retrieve().bodyToMono(Traces.class).block(timeout);
        logger.debug("traces [{}].",traces.toString());
        return traces;
    }

    public Batches selectBatches(String traceId){
        Batches batches =  WebClient.create("http://0.0.0.0:3200/api/traces/" + traceId).get().retrieve().bodyToMono(Batches.class).block(timeout);
        logger.debug("batches [{}].", batches.toString());
        return batches;
    }

}

