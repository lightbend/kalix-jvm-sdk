package com.example.domain;

import com.example.CounterApi;
import com.example.Main;
import com.example.CounterServiceClient;
import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import org.junit.ClassRule;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.*;

import static java.util.concurrent.TimeUnit.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pfailsafe`
public class CounterIntegrationTest {
    
    /**
     * The test kit starts both the service container and the Akka Serverless proxy.
     */
    @ClassRule
    public static final AkkaServerlessTestkitResource testkit = new AkkaServerlessTestkitResource(Main.SERVICE);
    
    /**
     * Use the generated gRPC client to call the service through the Akka Serverless proxy.
     */
    private final CounterServiceClient client;
    
    public CounterIntegrationTest() {
        client = CounterServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
    }
    
    @Test
    public void increaseOnNonExistingEntity() throws Exception {
        String entityId = "new-id";
        client.increase(CounterApi.IncreaseValue.newBuilder().setCounterId(entityId).setValue(42).build())
                 .toCompletableFuture().get(5, SECONDS);
        CounterApi.CurrentCounter reply = client.getCurrentCounter(CounterApi.GetCounter.newBuilder().setCounterId(entityId).build())
                .toCompletableFuture().get(2, SECONDS);
        assertThat(reply.getValue(), is(42));
    }
    
    @Test
    public void increase() throws Exception {
        String entityId = "another-id";
        client.increase(CounterApi.IncreaseValue.newBuilder().setCounterId(entityId).setValue(42).build())
                 .toCompletableFuture().get(5, SECONDS);
        client.increase(CounterApi.IncreaseValue.newBuilder().setCounterId(entityId).setValue(27).build())
                 .toCompletableFuture().get(2, SECONDS);
        CounterApi.CurrentCounter reply = client.getCurrentCounter(CounterApi.GetCounter.newBuilder().setCounterId(entityId).build())
                .toCompletableFuture().get(2, SECONDS);
        assertThat(reply.getValue(), is(69));
    }
}
