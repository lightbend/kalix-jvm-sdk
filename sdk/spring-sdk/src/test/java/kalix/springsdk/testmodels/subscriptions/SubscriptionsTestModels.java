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

package kalix.springsdk.testmodels.subscriptions;

import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.testmodels.Message;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class SubscriptionsTestModels {

  public static class SubscribeToValueEntityAction extends Action {

    @Subscribe.ValueEntity(entityType = "ve-counter")
    @PostMapping("/message/one")
    public Action.Effect<Message> messageOne(@RequestBody Message message) {
      return effects().reply(message);
    }

    @Subscribe.ValueEntity(entityType = "ve-counter")
    @PostMapping("/message/two")
    public Action.Effect<Message> messageTwo(@RequestBody Message message) {
      return effects().reply(message);
    }
  }
}
