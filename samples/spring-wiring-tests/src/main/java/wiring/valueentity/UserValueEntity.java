package wiring.valueentity;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.springsdk.annotations.Entity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import wiring.SomeComponent;
import wiring.domain.User;


@Entity(entityKey = "id", entityType = "user")
@RequestMapping("/user/{id}")
public class UserValueEntity extends ValueEntity<User> {

  private final ValueEntityContext context;
  private final SomeComponent someComponent;

  public UserValueEntity(ValueEntityContext context, SomeComponent someComponent) {
    this.context = context;
    this.someComponent = someComponent;
  }

  @PostMapping("/{name}/{email}")
  public Effect<String> createUser(@PathVariable String name, @PathVariable String email) {
    return effects().updateState(new User(name, email)).thenReply("Ok");
  }
}
