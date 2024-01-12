package com.example.cinema;


import java.util.UUID;

import static com.example.cinema.DomainGenerators.randomSeatNumber;
import static com.example.cinema.DomainGenerators.randomTitle;
import static com.example.cinema.Show.ShowCommand.*;

public class ShowCommandGenerators {

  public static CreateShow randomCreateShow() {
    return new CreateShow(randomTitle(), ShowBuilder.MAX_SEATS);
  }

  public static ReserveSeat randomReserveSeat() {
    return new ReserveSeat(UUID.randomUUID().toString(), UUID.randomUUID().toString(), randomSeatNumber());
  }

  public static CancelSeatReservation randomCancelSeatReservation() {
    return new CancelSeatReservation(UUID.randomUUID().toString());
  }
}
