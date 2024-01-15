package com.example.cinema;

import com.example.cinema.reservation.ReservationEntity;
import com.example.common.Response;
import com.example.wallet.WalletEntity;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

import static com.example.wallet.Wallet.WalletEvent.WalletChargeRejected;
import static com.example.wallet.Wallet.WalletEvent.WalletCharged;

@Subscribe.EventSourcedEntity(value = WalletEntity.class, ignoreUnknown = true)
public class CompleteReservationAction extends Action {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public CompleteReservationAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Response> confirmReservation(WalletCharged walletCharged) {
    logger.info("confirming reservation, triggered by " + walletCharged);

    String reservationId = walletCharged.expenseId();

    return effects().asyncReply(
      getShowIdBy(reservationId).thenCompose(showId ->
        confirmReservation(showId, reservationId)
      ));
  }

  public Effect<Response> cancelReservation(WalletChargeRejected walletChargeRejected) {
    logger.info("cancelling reservation, triggered by " + walletChargeRejected);

    String reservationId = walletChargeRejected.expenseId();

    return effects().asyncReply(
      getShowIdBy(reservationId).thenCompose(showId ->
        cancelReservation(showId, reservationId)
      ));
  }

  private CompletionStage<Response> confirmReservation(String showId, String reservationId) {
    return componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::confirmPayment)
      .params(reservationId)
      .execute();
  }

  private CompletionStage<Response> cancelReservation(String showId, String reservationId) {
    return componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::cancelReservation)
      .params(reservationId)
      .execute();
  }

  //Value Entity as a read model
  private CompletionStage<String> getShowIdBy(String reservationId) {
    return componentClient.forValueEntity(reservationId).call(ReservationEntity::get).execute()
      .thenApply(Show.Reservation::showId);
  }
}
