package com.example.cinema;

import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import kalix.javasdk.view.View;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static com.example.cinema.Show.ShowsByAvailableSeatsRecordList;
import static com.example.cinema.Show.ShowsByAvailableSeatsViewRecord;
import static com.example.cinema.Show.ShowEvent.*;
@ViewId("show_by_available_seats_view")
@Table("show_by_available_seats")
@Subscribe.EventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ShowsByAvailableSeatsView extends View<ShowsByAvailableSeatsViewRecord> {

  @GetMapping("/cinema-shows/by-available-seats/{requestedSeatCount}")
  @Query("SELECT * as list FROM show_by_available_seats WHERE availableSeats >= :requestedSeatCount")
  public ShowsByAvailableSeatsRecordList getShows(@PathVariable Integer requestedSeatCount) {
    return null;
  }

  public UpdateEffect<ShowsByAvailableSeatsViewRecord> onEvent(ShowCreated created) {
    return effects().updateState(new ShowsByAvailableSeatsViewRecord(created.showId(), created.title(), created.seats().size()));
  }

  public UpdateEffect<ShowsByAvailableSeatsViewRecord> onEvent(SeatReserved reserved) {
    return effects().updateState(viewState().updateAvailableSeats(reserved.availableSeatsCount()));
  }
  public UpdateEffect<ShowsByAvailableSeatsViewRecord> onEvent(SeatReservationCancelled cancelled) {
    return effects().updateState(viewState().updateAvailableSeats(cancelled.availableSeatsCount()));
  }
}

