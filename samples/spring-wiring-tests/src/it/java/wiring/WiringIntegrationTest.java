package wiring;

import kalix.springsdk.KalixConfigurationTest;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import wiring.action.Message;
import wiring.domain.User;

import java.time.Duration;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@Import(KalixConfigurationTest.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class WiringIntegrationTest {


  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void verifyActionWiring() {

    Message response =
        webClient.get()
            .uri("/echo/message/abc")
            .retrieve()
            .bodyToMono(Message.class)
            .block(timeout);

    Assertions.assertEquals("abc", response.text);

  }

  @Test
  public void verifyEntityAndViewWiring() {

    ResponseEntity<String> response =
        webClient.post()
            .uri("/user/abc/joe/joe%40example.com")
            .retrieve()
            .toEntity(String.class)
            .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

    // the view is eventually updated
    await()
        .atMost(20, TimeUnit.SECONDS)
        .until(() ->
              webClient.get()
                  .uri("/users/by_name/joe")
                  .retrieve()
                  .bodyToMono(User.class)
                  .block(timeout)
                  .name,
            new IsEqual("joe")
            );
  }

}
