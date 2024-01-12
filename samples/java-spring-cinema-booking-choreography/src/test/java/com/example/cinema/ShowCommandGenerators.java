package com.example.cinema;


import static com.example.cinema.DomainGenerators.*;
import static com.example.cinema.Show.ShowCommand.*;

public class ShowCommandGenerators {

  public static CreateShow randomCreateShow() {
    return new CreateShow(randomTitle(), ShowBuilder.MAX_SEATS);
  }

  public static ReserveSeat randomReserveSeat() {
    return new ReserveSeat(randomWalletId(), randomReservationId(), randomSeatNumber());
  }

  public static CancelSeatReservation randomCancelSeatReservation() {
    return new CancelSeatReservation(randomReservationId());
  }
}
