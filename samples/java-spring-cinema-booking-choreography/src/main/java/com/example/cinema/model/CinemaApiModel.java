package com.example.cinema.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

public interface CinemaApiModel {
    sealed interface ShowCommand {

      record CreateShow(String title, int maxSeats) implements ShowCommand {}

      record ReserveSeat(String walletId, String reservationId, int seatNumber) implements ShowCommand {}

      record ConfirmReservationPayment(String reservationId) implements ShowCommand {}

          record CancelSeatReservation(String reservationId) implements ShowCommand {}
    }

    enum ShowCommandError {
      SHOW_ALREADY_EXISTS,
      SHOW_NOT_FOUND,
      TOO_MANY_SEATS,
      SEAT_NOT_FOUND,
      SEAT_NOT_AVAILABLE,
      RESERVATION_NOT_FOUND,
      DUPLICATED_COMMAND,
      CANCELLING_CONFIRMED_RESERVATION
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
      @JsonSubTypes.Type(value = Response.Success.class),
      @JsonSubTypes.Type(value = Response.Failure.class)
    })
    sealed interface Response {

      record Success(String message) implements Response {
        public static Success of(String message) {
          return new Success(message);
        }
      }

      record Failure(String message) implements Response {
        public static Failure of(String message) {
          return new Failure(message);
        }
      }
    }

    record ShowResponse(String id, String title, List<Show.Seat> seats) {

      public static ShowResponse from(Show show) {
        return new ShowResponse(show.id(), show.title(), show.seats().values().asJava());
      }
    }

    record GetShowsByAvailableSeatsCommand(Integer requestedSeatCount){}
    record ShowsByAvailableSeatsViewRecord(String showId, String title, int availableSeats){
        public ShowsByAvailableSeatsViewRecord updateAvailableSeats(int availableSeats){
            return new ShowsByAvailableSeatsViewRecord(showId(),title(),availableSeats);
        }
    }
    record ShowsByAvailableSeatsRecordList(List<ShowsByAvailableSeatsViewRecord> list){}
}
