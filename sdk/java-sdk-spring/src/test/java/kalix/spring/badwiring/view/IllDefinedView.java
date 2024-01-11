/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
