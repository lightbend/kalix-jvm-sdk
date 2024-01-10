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

package com.example.wiring.actions.echo;

import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.CompletableFuture;

public class ActionWithMetadata extends Action {

  private ComponentClient componentClient;

  public ActionWithMetadata(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @GetMapping("/action-with-meta/{key}/{value}")
  public Effect<Message> actionWithMeta(@PathVariable String key, @PathVariable String value) {
    var def = componentClient.forAction().call(ActionWithMetadata::returnMeta).params(key);
    return effects().forward(def.withMetadata(Metadata.EMPTY.add(key, value)));
  }

  @GetMapping("/return-meta/{key}")
  public Effect<Message> returnMeta(@PathVariable String key) {
    var metaValue = actionContext().metadata().get(key).get();
    return effects().reply(new Message(metaValue));
  }

  @GetMapping("/reply-meta/{key}/{value}")
  public Effect<Message> returnAsMeta(@PathVariable String key, @PathVariable String value) {
    var md = Metadata.EMPTY.add(key, value);
    return effects().reply(new Message(value), md);
  }

  @GetMapping("/reply-async-meta/{key}/{value}")
  public Effect<Message> returnAsMetaAsync(@PathVariable String key, @PathVariable String value) {
    var md = Metadata.EMPTY.add(key, value);
    return effects().asyncReply(CompletableFuture.completedFuture(new Message(value)), md);
  }
}
