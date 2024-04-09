/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.badwiring.view;

import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Table;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

@Table("test")
@Component
public class IllDefinedView extends View<IllDefinedView.DummyUser> {

  record DummyUser(String s){}

  @Query("SELECT * FROM users_view WHERE email = :email")
  @GetMapping("/users/{email}")
  public DummyUser getUser(String email) {
    return null; // TODO: user should not implement this. we need to find a nice API for this
  }

}
