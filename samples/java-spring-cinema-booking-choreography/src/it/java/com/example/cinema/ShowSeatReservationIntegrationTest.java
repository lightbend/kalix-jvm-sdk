package com.example.cinema;

import com.example.Main;
import com.example.common.Response;
import com.example.wallet.Wallet;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.wallet.Wallet.WalletCommand.ChargeWallet;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


@DirtiesContext
@SpringBootTest(classes = Main.class)
public class ShowSeatReservationIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;
  @Autowired
  private ShowCalls calls;

  @Autowired
  private WalletCalls walletCalls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldCompleteSeatReservation() throws Exception{
    //given
    var walletId = TestUtils.randomId();
    var showId = TestUtils.randomId();
    var reservationId = TestUtils.randomId();
    var seatNumber = 10;

    walletCalls.createWallet(walletId, 200);
    calls.createShow(showId, "pulp fiction");

    //when
    Response reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse).isInstanceOf(Response.Success.class);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        Show.SeatStatus seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(Show.SeatStatus.PAID);

        Wallet.WalletResponse wallet = walletCalls.getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(100));
      });
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() throws Exception{
    //given
    var walletId = TestUtils.randomId();
    var showId = TestUtils.randomId();
    var reservationId = TestUtils.randomId();
    var seatNumber = 11;

    walletCalls.createWallet(walletId, 1);
    calls.createShow(showId, "pulp fiction");

    //when
    Response reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse).isInstanceOf(Response.Success.class);

    //then
    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        Show.SeatStatus seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(Show.SeatStatus.AVAILABLE);
      });
  }

  @Test
  public void shouldAllowToCancelAlreadyCancelledReservation() throws Exception{
    //given
    var walletId = TestUtils.randomId();
    var showId = TestUtils.randomId();
    var reservationId = "42";
    var seatNumber = 11;

    walletCalls.createWallet(walletId, 300);
    calls.createShow(showId, "pulp fiction");

    //when
    Response reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse).isInstanceOf(Response.Success.class);

    //then
    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        Show.SeatStatus seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(Show.SeatStatus.AVAILABLE);
      });

    //simulating that the wallet charging was rejected for this reservation
    walletCalls.chargeWallet(walletId, new ChargeWallet(new BigDecimal(400), reservationId));

    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        Wallet.WalletResponse wallet = walletCalls.getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(300));
      });
  }
}