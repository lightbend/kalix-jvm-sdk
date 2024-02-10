package com.example.tracing;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;


// TODO: this was added just for testing the auto-instrumentation
// we should remove it and allow the user to use an injected OpenTelemetry instance coming from Telemetry
@Configuration
public class OpenTelemetryConfig {

  @Bean
  public OpenTelemetry openTelemetry() {
    return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  }
}
