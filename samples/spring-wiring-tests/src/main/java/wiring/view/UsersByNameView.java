package wiring.view;

import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewCreationContext;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;
import wiring.SomeComponent;
import wiring.domain.User;
import wiring.valueentity.UserValueEntity;


@Table("users_by_name")
@Subscribe.ValueEntity(UserValueEntity.class)
public class UsersByNameView extends View<User> {

  private final ViewCreationContext context;
  private SomeComponent someComponent;

  public UsersByNameView(ViewCreationContext context, SomeComponent someComponent) {
    this.context = context;
    this.someComponent = someComponent;
  }

  @GetMapping("/users/by_name/{name}")
  @Query("SELECT * FROM users_by_name WHERE name = :name")
  public User getUser(String name) {
    return null;
  }

}
