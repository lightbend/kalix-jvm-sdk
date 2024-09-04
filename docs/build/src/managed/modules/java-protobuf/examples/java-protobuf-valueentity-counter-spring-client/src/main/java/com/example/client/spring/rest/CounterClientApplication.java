package com.example.client.spring.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// tag::counterApplication[]
@SpringBootApplication
public class CounterClientApplication {
  public static void main(String[] args) {
    SpringApplication.run(CounterClientApplication.class, args);
  }
}
// end::counterApplication[]
