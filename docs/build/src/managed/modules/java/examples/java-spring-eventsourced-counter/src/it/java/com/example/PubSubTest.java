package com.example;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

public class PubSubTest {

  public static void main(String[] args) {
    var projectId = "test";

    // Create a WebClient instance
    WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:8085") // Replace with your Pub/Sub emulator URL
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();

    // Check for subscriptions
    Mono<SubscriptionList> subscriptionListMono = webClient.get()
        .uri("/v1/projects/{projectId}/subscriptions", projectId) // Replace with your project ID
        .retrieve()
        .bodyToMono(SubscriptionList.class);

    // Consume messages from a subscription
    subscriptionListMono.flatMapMany(subscriptionList -> {
      System.out.println(subscriptionList);
      String subscriptionName = "counter-commands_com.example.actions.CounterCommandFromTopic"; // Replace with your subscription name
      return webClient.get()
          .uri("/v1/projects/{projectId}/subscriptions/{subscriptionName}:pull", projectId, subscriptionName)
          .retrieve()
          .bodyToMono(PullResponse.class)
          .flatMapIterable(PullResponse::getReceivedMessages)
          .map(ReceivedMessage::getMessage)
          .map(Message::getData);
    }).subscribe(System.out::println); // Process the received messages as needed

    subscriptionListMono.block();
  }

  // POJO classes for deserialization
  static class SubscriptionList {
    public List<Subscription> getSubscriptions() {
      return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
      this.subscriptions = subscriptions;
    }

    List<Subscription> subscriptions;

    @Override
    public String toString() {
      return "SubscriptionList{" +
          "subscriptions=" + subscriptions +
          '}';
    }
  }

  static class Subscription {
    // Define fields according to the Pub/Sub API response structure
    String name;
    String topic;

    @Override
    public String toString() {
      return "Subscription{" +
          "name='" + name + '\'' +
          ", topic='" + topic + '\'' +
          '}';
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }
  }


  static class PullResponse {
    // Define fields according to the Pub/Sub API response structure
    List<ReceivedMessage> receivedMessages;

    public List<ReceivedMessage> getReceivedMessages() {
      return receivedMessages;
    }
  }

  static class ReceivedMessage {
    // Define fields according to the Pub/Sub API response structure
    Message message;

    public Message getMessage() {
      return message;
    }
  }

  static class Message {
    // Define fields according to the Pub/Sub API response structure
    String data;

    public String getData() {
      return data;
    }
  }
}
