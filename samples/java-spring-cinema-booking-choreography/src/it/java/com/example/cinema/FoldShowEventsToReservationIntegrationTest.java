package com.example.cinema;

import com.example.Main;
import com.example.cinema.model.Show;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.cinema.TestUtils.randomId;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
class FoldShowEventsToReservationIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;
  @Autowired
  private ShowCalls calls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldUpdateShowByReservationEntry() {
    //given
    var showId = randomId();
    var reservationId1 = randomId();
    var reservationId2 = randomId();
    var walletId = randomId();
    calls.createShow(showId, "title");

    //when
    calls.reserveSeat(showId, walletId, reservationId1, 3);
    calls.reserveSeat(showId, walletId, reservationId2, 4);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        Show.Reservation result = getReservation(reservationId1).getBody();
        assertThat(result).isEqualTo(new Show.Reservation(reservationId1, showId, walletId, new BigDecimal(100)));

        Show.Reservation result2 = getReservation(reservationId2).getBody();
        assertThat(result2).isEqualTo(new Show.Reservation(reservationId2, showId, walletId, new BigDecimal(100)));
      });
  }

  private ResponseEntity<Show.Reservation> getReservation(String reservationId) {
    return webClient.get().uri("/reservation/" + reservationId)
      .retrieve()
      .toEntity(Show.Reservation.class)
      .onErrorResume(WebClientResponseException.class, error -> {
        if (error.getStatusCode().is4xxClientError()) {
          return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else {
          return Mono.error(error);
        }
      })
      .block();
  }
}