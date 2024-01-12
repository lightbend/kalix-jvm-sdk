package com.example.cinema;

import com.example.common.Response;
import kalix.javasdk.client.ComponentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class ShowCalls {

  @Autowired
  private ComponentClient componentClient;
  private int timeout = 10;
  public void createShow(String showId, String title) throws Exception {
     createShow(showId,title,100);
  }
  public void createShow(String showId, String title, int maxSeats) throws Exception{
    componentClient.forEventSourcedEntity(showId).call(ShowEntity::create).params(showId, new Show.ShowCommand.CreateShow(title, maxSeats)).execute().toCompletableFuture().get(timeout, TimeUnit.SECONDS);
  }

  public Show.SeatStatus getSeatStatus(String showId, int seatNumber) throws Exception{
    return componentClient.forEventSourcedEntity(showId).call(ShowEntity::getSeatStatus).params(seatNumber).execute().toCompletableFuture().get(timeout,TimeUnit.SECONDS);
  }

  public Response reserveSeat(String showId, String walletId, String reservationId, int seatNumber) throws Exception{
    return componentClient.forEventSourcedEntity(showId).call(ShowEntity::reserve).params(new Show.ShowCommand.ReserveSeat(walletId, reservationId, seatNumber)).execute().toCompletableFuture().get(timeout,TimeUnit.SECONDS);
  }

  public Response cancelSeatReservation(String showId, String reservationId) throws Exception{
    return componentClient.forEventSourcedEntity(showId).call(ShowEntity::cancelReservation).params(reservationId).execute().toCompletableFuture().get(timeout,TimeUnit.SECONDS);
  }

  public Show.ShowsByAvailableSeatsRecordList getShowsByAvailableSeats(int requestedSeatCount) throws Exception{
    return componentClient.forView().call(ShowsByAvailableSeatsView::getShows).params(requestedSeatCount).execute().toCompletableFuture().get(timeout,TimeUnit.SECONDS);
  }

}
