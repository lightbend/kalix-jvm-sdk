package com.example.client.spring.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

// tag::webclientconfig[]
@Configuration
public class WebClientConfig {

  @Value("${as.host}")
  String host;

  @Value("${as.port}")
  int port;

  @Bean
  public WebClient createWebClient() {
    return WebClient.builder().baseUrl("http://" + host + ":" + port + "")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
// end::webclientconfig[]