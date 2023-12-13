package com.example.cinema;

import com.example.cinema.model.CinemaApiModel;
import com.example.cinema.model.Show;
import com.example.cinema.model.ShowEvent;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import static com.example.cinema.DomainGenerators.*;
import static com.example.cinema.model.Show.SeatStatus.PAID;
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
    EventSourcedTestKit<Show, ShowEvent, ShowEntity> testKit = EventSourcedTestKit.of(ShowEntity::new);
    var createShow = new CinemaApiModel.ShowCommand.CreateShow("title", maxSeats);
    var reserveSeat = new CinemaApiModel.ShowCommand.ReserveSeat(walletId, reservationId, seatNumber);

    //when
    testKit.call(s -> s.create(showId, createShow));
    testKit.call(s -> s.reserve(reserveSeat));
    EventSourcedResult<CinemaApiModel.Response> result = testKit.call(s -> s.confirmPayment(reservationId));

    //then
    var confirmedSeat = testKit.getState().seats().get(seatNumber).get();
    assertThat(confirmedSeat.number()).isEqualTo(seatNumber);
    assertThat(confirmedSeat.status()).isEqualTo(PAID);

    var availableSeats = testKit.getState().availableSeats();
    assertThat(availableSeats).isEqualTo(maxSeats-1);
  }
}