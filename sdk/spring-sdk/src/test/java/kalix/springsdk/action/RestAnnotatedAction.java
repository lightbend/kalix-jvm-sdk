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

package kalix.springsdk.action;

import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.*;

public class RestAnnotatedAction extends Action {

  @PostMapping("/post/message")
  public Effect<Message> postNumber(Message msg) {
    return effects().reply(msg);
  }

  @GetMapping("/get/message")
  public Effect<Message> getMessage(Message msg) {
    return effects().reply(msg);
  }

  @PutMapping("/put/message")
  public Effect<Message> putMessage(Message msg) {
    return effects().reply(msg);
  }

  @PatchMapping("/patch/message")
  public Effect<Message> patchMessage(Message msg) {
    return effects().reply(msg);
  }
}
