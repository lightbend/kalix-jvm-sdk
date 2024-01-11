/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wiring.pubsub;

import kalix.javasdk.testkit.KalixTestKit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.example.wiring.pubsub.PublishESToTopic.COUNTER_EVENTS_TOPIC;
import static com.example.wiring.pubsub.PublishVEToTopic.CUSTOMERS_TOPIC;
import static java.time.Duration.ofMillis;

@Configuration
public class TestkitConfigEventing {

  @Bean("settings")
  @Profile("eventing-testkit-destination")
  public KalixTestKit.Settings settingsMockedDestination() {
    // here only to show how to set different `Settings` in a test. See SpringSdkIntegrationTest.java
    return KalixTestKit.Settings.DEFAULT
        .withAclEnabled()
        .withAdvancedViews()
        .withWorkflowTickInterval(ofMillis(500))
        .withTopicOutgoingMessages(CUSTOMERS_TOPIC);
  }

  @Bean("settings")
  @Profile("eventing-testkit-subscription")
  public KalixTestKit.Settings settingsMockedSubscription() {
    // here only to show how to set different `Settings` in a test. See SpringSdkIntegrationTest.java
    return KalixTestKit.Settings.DEFAULT
        .withAclEnabled()
        .withAdvancedViews()
        .withWorkflowTickInterval(ofMillis(500))
        .withTopicIncomingMessages(COUNTER_EVENTS_TOPIC)
        .withValueEntityIncomingMessages("user");
  }
}
