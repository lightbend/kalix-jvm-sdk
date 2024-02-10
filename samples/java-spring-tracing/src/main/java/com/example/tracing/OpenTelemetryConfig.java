package com.example.tracing;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;


@Configuration
public class OpenTelemetryConfig {

  @Bean
  public OpenTelemetry openTelemetry() {
    return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  }
}
