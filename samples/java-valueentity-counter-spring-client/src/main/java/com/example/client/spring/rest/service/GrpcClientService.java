package com.example.client.spring.rest.service;

import com.example.CounterApi;
import com.example.CounterServiceGrpc;
import com.example.client.spring.rest.model.ValueRequest;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// tag::decreaseCounterCall[]
@Service
public class GrpcClientService {

  @Autowired
  ManagedChannel channel;

  public String decreaseCounter(String counterId, ValueRequest valueRequest) {

    CounterServiceGrpc.CounterServiceBlockingStub counterServiceBlockingStub =
        CounterServiceGrpc.newBlockingStub(channel);

    Empty decreaseResponse = counterServiceBlockingStub.decrease(CounterApi.DecreaseValue.newBuilder()
        .setCounterId(counterId)
        .setValue(valueRequest.getValue())
        .build());

    return decreaseResponse.toString();
  }
}
// end::decreaseCounterCall[]
