package com.example;

public sealed interface CounterEvent {

  record ValueIncreased(int value) implements CounterEvent {
  }

  record ValueMultiplied(int value) implements CounterEvent {
  }
}
