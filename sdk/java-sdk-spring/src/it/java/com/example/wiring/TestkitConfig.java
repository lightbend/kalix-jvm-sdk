/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring;

import kalix.javasdk.testkit.KalixTestKit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.time.Duration.ofMillis;

@Configuration
public class TestkitConfig {

  @Bean
  public KalixTestKit.Settings settings() {
    // here only to show how to set different `Settings` in a test. See SpringSdkIntegrationTest.java
    return KalixTestKit.Settings.DEFAULT
        .withAclEnabled()
        .withAdvancedViews()
        .withWorkflowTickInterval(ofMillis(500));
  }
}
