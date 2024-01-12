package com.example.cinema;

import com.example.Main;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
class ShowsByAvailableSeatsViewIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private ShowCalls calls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldUpdateShowByAvailableSeatsEntry() throws Exception{
    //given
    var showId = TestUtils.randomId();
    var showTitle = "title";
    var showTitleSearch = showTitle;
    var maxSeats = 100;
    var reservationId1 = TestUtils.randomId();
    var reservationId2 = TestUtils.randomId();
    var walletId = TestUtils.randomId();
    calls.createShow(showId, showTitle, maxSeats);

    //when
    calls.reserveSeat(showId, walletId, reservationId1, 3);
    calls.reserveSeat(showId, walletId, reservationId2, 4);

    //then
    Show.ShowsByAvailableSeatsRecordList expected = new Show.ShowsByAvailableSeatsRecordList(List.of(new Show.ShowsByAvailableSeatsViewRecord(showId,showTitle,maxSeats-2)));
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        Show.ShowsByAvailableSeatsRecordList result = calls.getShowsByAvailableSeats(1);
        assertThat(expected).isEqualTo(result);

      });
  }

}