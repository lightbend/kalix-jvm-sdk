package com.example.client.spring.rest.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// tag::grpcclientconfig[]
@Configuration
public class GrpcClientConfig {

  @Value("${as.host}")
  String host;

  @Value("${as.port}")
  int port;

  @Bean
  public ManagedChannel createGrpcClient() {
    return ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build();
  }
}
// end::grpcclientconfig[]