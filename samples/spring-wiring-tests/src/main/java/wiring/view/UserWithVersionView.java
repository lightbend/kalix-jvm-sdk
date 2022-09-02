package wiring.view;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.*;
import wiring.domain.User;
import wiring.valueentity.UserValueEntity;


@Table("users_view")
public class UserWithVersionView extends View<UserWithVersion> {

  // when methods are annotated, it's implicitly a transform = true
  @Subscribe.ValueEntity(UserValueEntity.class)
  public UpdateEffect<UserWithVersion> onChange(UserWithVersion userView, User user) {
      return effects().updateState(new UserWithVersion(user.name, user.email, userView.version + 1));
  }

  @Query("SELECT * FROM users_view WHERE email = :email")
  @PostMapping("/users/by-email/{email}")
  public UserWithVersion getUser(@PathVariable String email) {
    return null;
  }

  @Override
  public UserWithVersion emptyState() {
    return new UserWithVersion("", "", 0);
  }
}
