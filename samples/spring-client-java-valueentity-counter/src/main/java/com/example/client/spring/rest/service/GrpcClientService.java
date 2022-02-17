package com.example.client.spring.rest.service;

import com.example.CounterApi;
import com.example.CounterServiceGrpc;
import com.example.client.spring.rest.model.CounterRequest;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrpcClientService {

    @Value("${as.host}")
    String host;

    @Value("${as.port}")
    int port;

    public String decrease(CounterRequest counterRequest) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        CounterServiceGrpc.CounterServiceBlockingStub counterServiceBlockingStub =
                CounterServiceGrpc.newBlockingStub(channel);

        Empty decreaseResponse = counterServiceBlockingStub.decrease(CounterApi.DecreaseValue.newBuilder()
                .setCounterId(counterRequest.getCounterId())
                .setValue(counterRequest.getValue())
                .build());

        channel.shutdown();

        return decreaseResponse.toString();
    }


}
