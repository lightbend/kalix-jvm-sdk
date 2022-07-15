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

package kalix.springsdk.wiring;

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class EchoAction extends Action {

  private Parrot parrot;
  private ActionCreationContext ctx;

  public EchoAction(Parrot parrot, ActionCreationContext ctx) {
    this.parrot = parrot;
    this.ctx = ctx;
  }

  public boolean hasContext() {
    return ctx != null;
  }

  public boolean hasParrot() {
    return parrot != null;
  }

  @GetMapping("/echo/{msg}")
  public Effect<String> stringMessage(@PathVariable String msg) {
    return effects().reply(this.parrot.repeat(msg));
  }
}
