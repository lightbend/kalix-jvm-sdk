package com.example.cinema;

import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import static com.example.cinema.DomainGenerators.*;
import static com.example.cinema.Show.SeatStatus.PAID;
import static org.assertj.core.api.Assertions.assertThat;

class ShowEntityTest {

  @Test
  public void shouldReserveAndConfirmSeat() {
    //given
    var showId = randomShowId();
    var walletId = randomWalletId();
    var reservationId = randomReservationId();
    var maxSeats = 100;
    int seatNumber = 1;
    EventSourcedTestKit<Show, Show.ShowEvent, ShowEntity> testKit = EventSourcedTestKit.of(ShowEntity::new);
    var createShow = new Show.ShowCommand.CreateShow("title", maxSeats);
    var reserveSeat = new Show.ShowCommand.ReserveSeat(walletId, reservationId, seatNumber);

    //when
    EventSourcedResult<Show.Response> result = testKit.call(s -> s.create(showId, createShow));
    var event = result.getNextEventOfType(Show.ShowEvent.ShowCreated.class);
    assertThat(event.seats().size()).isEqualTo(maxSeats);

    testKit.call(s -> s.reserve(reserveSeat));
    testKit.call(s -> s.confirmPayment(reservationId));

    //then
    var confirmedSeat = testKit.getState().seats().get(seatNumber).get();
    assertThat(confirmedSeat.number()).isEqualTo(seatNumber);
    assertThat(confirmedSeat.status()).isEqualTo(PAID);

    var availableSeats = testKit.getState().availableSeats();
    assertThat(availableSeats).isEqualTo(maxSeats-1);
  }
}