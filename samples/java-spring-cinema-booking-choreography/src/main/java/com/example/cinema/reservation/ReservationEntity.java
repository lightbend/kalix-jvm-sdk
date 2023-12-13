package com.example.cinema.reservation;

import com.example.cinema.model.Show;
import io.grpc.Status;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.valueentity.ValueEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Id("id")
@TypeId("reservation")
@RequestMapping("/reservation/{id}")
public class ReservationEntity extends ValueEntity<Show.Reservation> {

  public record CreateReservation(String showId, String walletId, BigDecimal price) {
  }

  @GetMapping
  public Effect<Show.Reservation> get() {
    if (currentState() == null) {
      return effects().error("reservation not found", Status.Code.NOT_FOUND);
    } else {
      return effects().reply(currentState());
    }
  }

  @PostMapping
  public Effect<String> create(@RequestBody CreateReservation createReservation) {
    String reservationId = commandContext().entityId();
    Show.Reservation reservation = new Show.Reservation(reservationId, createReservation.showId, createReservation.walletId, createReservation.price);
    return effects().updateState(reservation).thenReply("reservation created");
  }

  @DeleteMapping
  public Effect<String> delete() {
    return effects().deleteEntity().thenReply("reservation deleted");
  }

  public static final record ShowByReservation(String showId, Set<String> reservationIds) {

      public ShowByReservation(String showId) {
        this(showId, new HashSet<>());
      }

      public ShowByReservation add(String reservationId) {
        if (!reservationIds.contains(reservationId)) {
          reservationIds.add(reservationId);
        }
        return this;
      }

      public ShowByReservation remove(String reservationId) {
        reservationIds.remove(reservationId);
        return this;
      }
    }
}
