package com.example.client.spring.rest.model;

public class CounterRequest {

  String counterId;

  int value;

  public String getCounterId() {
    return counterId;
  }

  public void setCounterId(String counterId) {
    this.counterId = counterId;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

}
