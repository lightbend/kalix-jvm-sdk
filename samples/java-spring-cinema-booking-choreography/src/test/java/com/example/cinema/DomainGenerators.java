package com.example.cinema;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

import static com.example.cinema.ShowBuilder.showBuilder;


public class DomainGenerators {

  private static final Random random = new Random();

  public static BigDecimal randomPrice() {
    return BigDecimal.valueOf(random.nextInt(200) + 50);
  }

  public static String randomTitle() {
    return "show-title-" + UUID.randomUUID().toString().substring(0, 8);
  }

  public static int randomSeatNumber() {
    return random.nextInt(ShowBuilder.MAX_SEATS);
  }

  public static Show randomShow() {
    return showBuilder().withRandomSeats().build();
  }
}
