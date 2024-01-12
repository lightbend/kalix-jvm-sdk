package com.example.cinema;

import io.vavr.Tuple2;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.example.cinema.DomainGenerators.randomShow;
import static com.example.cinema.Show.SeatStatus.*;
import static com.example.cinema.Show.ShowCommand.CancelSeatReservation;
import static com.example.cinema.Show.ShowCommand.ConfirmReservationPayment;
import static com.example.cinema.Show.ShowCommandError.*;
import static com.example.cinema.Show.ShowEvent.*;
import static com.example.cinema.ShowBuilder.showBuilder;
import static com.example.cinema.ShowCommandGenerators.randomCreateShow;
import static com.example.cinema.ShowCommandGenerators.randomReserveSeat;
import static io.vavr.control.Either.left;
import static org.assertj.core.api.Assertions.assertThat;

class ShowTest {

  @Test
  public void shouldCreateTheShow() {
    //given
    String showId = UUID.randomUUID().toString();
    var createShow = randomCreateShow();

    //when
    var showCreated = Show.ShowCreator.create(showId, createShow).get();
    var show = Show.create(showCreated);

    //then
    assertThat(show.id()).isEqualTo(showId);
    assertThat(show.title()).isEqualTo(createShow.title());
    assertThat(show.seats()).hasSize(createShow.maxSeats());
  }

  @Test
  public void shouldNotProcessCreateShowCommandForExistingShow() {
    //given
    var show = randomShow();
    var createShow = randomCreateShow();

    //when
    var error = onCommand(show, createShow).getLeft();

    //then
    assertThat(error).isEqualTo(SHOW_ALREADY_EXISTS);
  }

  @Test
  public void shouldReserveTheSeat() {
    //given
    var show = randomShow();
    var reserveSeat = randomReserveSeat();
    var seatToReserve = show.getSeat(reserveSeat.seatNumber()).get();

    //when
    var event = onCommand(show, reserveSeat).get();

    //then
    assertThat(event).isEqualTo(new SeatReserved(show.id(), reserveSeat.walletId(), reserveSeat.reservationId(), reserveSeat.seatNumber(), seatToReserve.price(),show.seats().size()-1));
  }

  @Test
  public void shouldReserveTheSeatWithApplyingEvent() {
    //given
    var show = randomShow();
    var reserveSeat = randomReserveSeat();

    //when
    var event = onCommand(show, reserveSeat).get();
    var updatedShow = onEvent(show, event);

    //then
    var reservedSeat = updatedShow.seats().get(reserveSeat.seatNumber()).get();
    assertThat(event).isInstanceOf(SeatReserved.class);
    assertThat(reservedSeat.status()).isEqualTo(RESERVED);
    assertThat(updatedShow.pendingReservations()).contains(new Tuple2<>(reserveSeat.reservationId(), reserveSeat.seatNumber()));
  }

  @Test
  public void shouldNotReserveAlreadyReservedSeat() {
    //given
    var show = randomShow();
    var reserveSeat = randomReserveSeat();
    var reserveTheSameSeat = new Show.ShowCommand.ReserveSeat(UUID.randomUUID().toString(), UUID.randomUUID().toString(), reserveSeat.seatNumber());

    //when
    var event = onCommand(show, reserveSeat).get();
    var updatedShow = onEvent(show, event);

    //then
    assertThat(event).isInstanceOf(SeatReserved.class);

    //when
    Show.ShowCommandError result = onCommand(updatedShow, reserveTheSameSeat).getLeft();

    //then
    assertThat(result).isEqualTo(SEAT_NOT_AVAILABLE);
  }

  @Test
  public void shouldRejectReservationDuplicate() {
    //given
    var show = randomShow();
    var reserveSeat = randomReserveSeat();

    //when
    var event = onCommand(show, reserveSeat).get();
    var updatedShow = onEvent(show, event);

    //then
    assertThat(event).isInstanceOf(SeatReserved.class);

    //when
    Show.ShowCommandError result = onCommand(updatedShow, reserveSeat).getLeft();

    //then
    assertThat(result).isEqualTo(DUPLICATED_COMMAND);
  }

  @Test
  public void shouldNotReserveNotExistingSeat() {
    //given
    var show = randomShow();
    var reserveSeat = new Show.ShowCommand.ReserveSeat(UUID.randomUUID().toString(), UUID.randomUUID().toString(), ShowBuilder.MAX_SEATS + 1);

    //when
    Show.ShowCommandError result = onCommand(show, reserveSeat).getLeft();

    //then
    assertThat(result).isEqualTo(SEAT_NOT_FOUND);
  }

  @Test
  public void shouldCancelSeatReservation() {
    //given
    var reservedSeat = new Show.Seat(2, Show.SeatStatus.RESERVED, new BigDecimal("123"));
    var reservationId =UUID.randomUUID().toString();
    var show = showBuilder().withRandomSeats().withSeatReservation(reservedSeat, reservationId).build();
    var cancelSeatReservation = new CancelSeatReservation(reservationId);

    //when
    var event = onCommand(show, cancelSeatReservation).get();
    var updatedShow = onEvent(show, event);

    //then
    assertThat(event).isEqualTo(new SeatReservationCancelled(show.id(), reservationId, reservedSeat.number(),show.seats().size()+1));
    assertThat(updatedShow.getSeat(reservedSeat.number()).get().status()).isEqualTo(AVAILABLE);
    assertThat(updatedShow.pendingReservations().get(reservationId).isEmpty()).isTrue();
  }

  @Test
  public void shouldConfirmSeatReservation() {
    //given
    var reservedSeat = new Show.Seat(2, Show.SeatStatus.RESERVED, new BigDecimal("123"));
    var reservationId = UUID.randomUUID().toString();
    var show = showBuilder().withRandomSeats().withSeatReservation(reservedSeat, reservationId).build();
    var confirmReservationPayment = new ConfirmReservationPayment(reservationId);

    //when
    var event = onCommand(show, confirmReservationPayment).get();
    var updatedShow = onEvent(show, event);

    //then
    assertThat(event).isEqualTo(new SeatReservationPaid(show.id(), reservationId, reservedSeat.number()));
    assertThat(updatedShow.getSeat(reservedSeat.number()).get().status()).isEqualTo(PAID);
    assertThat(updatedShow.pendingReservations().get(reservationId).isEmpty()).isTrue();
  }

  @Test
  public void shouldNotCancelReservationOfAvailableSeat() {
    //given
    var show = randomShow();
    var cancelSeatReservation = new CancelSeatReservation(UUID.randomUUID().toString());

    //when
    var result = onCommand(show, cancelSeatReservation).getLeft();

    //then
    assertThat(result).isEqualTo(RESERVATION_NOT_FOUND);
  }

  public Show onEvent(Show show, Show.ShowEvent event) {
    return switch (event) {
      case Show.ShowEvent.ShowCreated ignored ->
              throw new IllegalStateException("Show is already created, use Show.create instead.");
      case Show.ShowEvent.SeatReserved seatReserved -> show.onEvent(seatReserved);
      case Show.ShowEvent.SeatReservationPaid seatReservationPaid ->
              show.onEvent(seatReservationPaid);
      case Show.ShowEvent.SeatReservationCancelled seatReservationCancelled ->
              show.onEvent(seatReservationCancelled);
    };
  }


  public Either<Show.ShowCommandError, Show.ShowEvent> onCommand(Show show, Show.ShowCommand command) {
    return switch (command) {
      case Show.ShowCommand.CreateShow ignored -> left(SHOW_ALREADY_EXISTS);
      case Show.ShowCommand.ReserveSeat reserveSeat -> show.onCommand(reserveSeat);
      case Show.ShowCommand.ConfirmReservationPayment confirmReservationPayment ->
              show.onCommand(confirmReservationPayment);
      case Show.ShowCommand.CancelSeatReservation cancelSeatReservation ->
              show.onCommand(cancelSeatReservation);
    };
  }
}