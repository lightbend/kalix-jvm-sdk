/*
 * Copyright 2021 Lightbend Inc.
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

package com.example.wiring.valueentities.user;

import io.grpc.Status;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.springsdk.KalixClient;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/validuser/{user}")
public class ValidateUserAction extends Action {

  private ActionCreationContext ctx;
  private KalixClient kalixClient;

  public ValidateUserAction(ActionCreationContext ctx, KalixClient kalixClient) {
    this.ctx = ctx;
    this.kalixClient = kalixClient;
  }

  @PutMapping("/{email}/{name}")
  public Action.Effect<String> createOrUpdateUser(@PathVariable String user, @PathVariable String email, @PathVariable String name) {
    if (email.isEmpty() || name.isEmpty())
      return effects().error("No field can be empty", Status.Code.INVALID_ARGUMENT);

    var defCall = kalixClient.put("/user/" + user + "/" + email + "/" + name, "", String.class);
    return effects().forward(defCall);
  }

  @PatchMapping("/email/{email}")
  public Action.Effect<String> updateEmail(@PathVariable String user, @PathVariable String email) {
    if (email.isEmpty())
      return effects().error("No field can be empty", Status.Code.INVALID_ARGUMENT);

    var defCall = kalixClient.patch("/user/" + user + "/email/" + email, "", String.class);
    return effects().forward(defCall);
  }

  @DeleteMapping
  public Action.Effect<String> delete(@PathVariable String user) {
    var defCall = kalixClient.delete("/user/" + user, "", String.class);
    return effects().forward(defCall);
  }
}
