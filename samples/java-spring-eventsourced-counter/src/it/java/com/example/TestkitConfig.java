package com.example;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// tag::class[]
@Configuration
public class TestkitConfig {
  // end::class[]

  @Profile("with-mocked-eventing")
  // tag::acls[]
  @Bean
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT.withAclEnabled()
        .withTopicIncomingMessages("counter-commands")
        .withTopicOutgoingMessages("counter-events")  // <1>
        // end::acls[]
        .withTopicOutgoingMessages("counter-events-with-meta");
        // tag::acls[]
  }
  // end::acls[]

  @Profile("with-pubsub")
  // tag::pubsub[]
  @Bean
  public KalixTestKit.Settings settingsWithPubSub() {
    return KalixTestKit.Settings.DEFAULT.withAclEnabled()
        .withEventingSupport(EventingSupport.GOOGLE_PUBSUB);
  }
  // end::pubsub[]

// tag::class[]
}
// end::class[]
