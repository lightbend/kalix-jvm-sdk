package com.example.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate(OpenTelemetry openTelemetry) {

    RestTemplate restTemplate = new RestTemplate();
    SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);
    restTemplate.getInterceptors().add(telemetry.newInterceptor());

    return restTemplate;
  }
}