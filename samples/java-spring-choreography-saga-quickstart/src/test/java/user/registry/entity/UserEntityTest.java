package user.registry.entity;

import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;
import user.registry.domain.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserEntityTest {

  @Test
  public void testCreationAndUpdate() {
    var userTestKit = EventSourcedTestKit.of(__ -> new UserEntity());

    var creationRes = userTestKit.call(userEntity -> userEntity.createUser(new User.Create("John", "Belgium", "john@acme.com")));

    var created = creationRes.getNextEventOfType(User.UserWasCreated.class);
    assertThat(created.name()).isEqualTo("John");
    assertThat(created.email()).isEqualTo("john@acme.com");

    var updateRes = userTestKit.call(userEntity -> userEntity.changeEmail(new User.ChangeEmail("john.doe@acme.com")));
    var emailChanged = updateRes.getNextEventOfType(User.EmailAssigned.class);
    assertThat(emailChanged.newEmail()).isEqualTo("john.doe@acme.com");
  }

  @Test
  public void updateNonExistentUser() {
    var userTestKit = EventSourcedTestKit.of(__ -> new UserEntity());

    var updateRes = userTestKit.call(userService -> userService.changeEmail(new User.ChangeEmail("john.doe@acme.com")));
    assertThat(updateRes.isError()).isTrue();
    assertThat(updateRes.getError()).isEqualTo("User not found");

  }
}
