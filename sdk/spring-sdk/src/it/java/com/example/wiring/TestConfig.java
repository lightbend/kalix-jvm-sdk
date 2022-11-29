package com.example.wiring;

import kalix.javasdk.testkit.KalixTestKit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {
    @Bean
    public KalixTestKit.Settings settings() {
        return KalixTestKit.Settings.DEFAULT.withAclEnabled(); // here only to show how to set different `Settings` in a test. See SpringSdkIntegrationTest.java
    }
}
