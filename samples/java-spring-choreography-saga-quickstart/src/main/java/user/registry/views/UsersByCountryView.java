package user.registry.views;

import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import kalix.javasdk.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import user.registry.domain.User;
import user.registry.entity.UserEntity;

import java.util.List;

/**
 * A View to query users by country.
 */
@ViewId("view-users-by-newCountry")
@Table("users_by_country")
@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class UsersByCountryView extends View<UsersByCountryView.UserView> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public record UserView(String id, String name, String country, String email) {
    public UserView withEmail(String email) {
      return new UserView(id, name, country, email);
    }
  }

  public record UserList(List<UserView> users) {
  }

  @GetMapping("/users/by-country/{country}")
  @Query("SELECT * AS users FROM users_by_country WHERE country = :country")
  public UserList getUserByCountry(@PathVariable String country) {
    return null;
  }

  public UpdateEffect<UserView> onEvent(User.UserWasCreated evt) {
    logger.info("User was created: {}", evt);
    var currentId = updateContext().eventSubject().orElseThrow();
    return effects().updateState(new UserView(currentId, evt.name(), evt.country(), evt.email()));
  }

  public UpdateEffect<UserView> onEvent(User.EmailAssigned evt) {
    logger.info("User address changed: {}", evt);
    var updatedView = viewState().withEmail(evt.newEmail());
    return effects().updateState(updatedView);
  }
}
