package user.registry;

import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import user.registry.api.ApplicationController;
import user.registry.domain.User;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Java SDK.
 * <p>
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 * <p>
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@SpringBootTest(classes = Main.class)
public class UserCreationIntegrationTest extends KalixIntegrationTestKitSupport {


  private final Duration timeout = Duration.ofSeconds(3);

  /**
   * This is a test for a successful user creation.
   * User is correctly created and email is marked as confirmed.
   */
  @Test
  public void testSuccessfulUserCreation() throws Exception {
    var callGetEmailInfo =
      componentClient.forAction()
        .call(ApplicationController::getEmailInfo)
        .params("doe@acme.com");

    assertThat(callGetEmailInfo.execute())
      .succeedsWithin(timeout)
      .satisfies(res -> {
        assertThat(res.ownerId()).isEmpty();
        assertThat(res.status()).isEqualTo("NOT_USED");
      });

    var callCreateUser =
      componentClient.forAction()
        .call(ApplicationController::createUser)
        .params("001", new User.Create("John Doe", "US", "doe@acme.com"));

    assertThat(callCreateUser.execute()).succeedsWithin(timeout);

    // get email once more and check it's now confirmed
    await()
      .ignoreExceptions()
      .atMost(timeout)
      .untilAsserted(() -> {
        assertThat(callGetEmailInfo.execute())
          .succeedsWithin(timeout)
          .satisfies(res -> {
            assertThat(res.ownerId()).isNotEmpty();
            assertThat(res.status()).isEqualTo("CONFIRMED");
          });
      });

  }

  /**
   * This is a test for the failure scenario
   * The email is reserved, but we fail to create the user.
   * Timer will fire and cancel the reservation.
   */
  @Test
  public void testUserCreationFailureDueToInvalidInput() throws Exception {
    var callGetEmailInfo =
      componentClient.forAction()
        .call(ApplicationController::getEmailInfo)
        .params("invalid@acme.com");

    assertThat(callGetEmailInfo.execute())
      .succeedsWithin(timeout)
      .satisfies(res -> {
        assertThat(res.ownerId()).isEmpty();
        assertThat(res.status()).isEqualTo("NOT_USED");
      });

    var callCreateUser =
      componentClient.forAction()
        .call(ApplicationController::createUser)
        // this user creation will fail because user's name is not provided
        .params("002", new User.Create(null, "US", "invalid@acme.com"));

    assertThat(callCreateUser.execute()).failsWithin(timeout);

    // email will be reserved for a while, then it will be released
    await()
      .ignoreExceptions()
      .atMost(timeout)
      .untilAsserted(() -> {
        assertThat(callGetEmailInfo.execute())
          .succeedsWithin(timeout)
          .satisfies(res -> {
            assertThat(res.ownerId()).isNotEmpty();
            assertThat(res.status()).isEqualTo("RESERVED");
          });
      });

    await()
      .ignoreExceptions()
      // timer will fire in 3 seconds and un-reserve the email
      // see it/resources/application.conf for the configuration
      // we only start to polling after 3 seconds to give the timer a chance to fire
      .between(Duration.ofSeconds(3), Duration.ofSeconds(6))
      .untilAsserted(() -> {
        assertThat(callGetEmailInfo.execute())
          .succeedsWithin(timeout)
          .satisfies(res -> {
            assertThat(res.ownerId()).isEmpty();
            assertThat(res.status()).isEqualTo("NOT_USED");
          });
      });

  }
}