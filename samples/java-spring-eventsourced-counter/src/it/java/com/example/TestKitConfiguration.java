package com.example;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// tag::class[]
@Configuration
public class TestKitConfiguration {
  // end::class[]

  @Profile("with-mocked-eventing")
  // tag::acls[]
  // tag::eventing-config[]
  @Bean
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT
        // end::eventing-config[]
        .withAclEnabled() // <1>
        // end::acls[]
        // tag::eventing-config[]
        .withTopicIncomingMessages("counter-commands") // <1>
        .withTopicOutgoingMessages("counter-events") // <2>
        // end::eventing-config[]
        .withTopicOutgoingMessages("counter-events-with-meta");
    // tag::eventing-config[]
        // tag::acls[]
  }
  // end::eventing-config[]
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
