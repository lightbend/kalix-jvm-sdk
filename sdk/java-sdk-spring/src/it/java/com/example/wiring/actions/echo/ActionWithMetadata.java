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

package com.example.wiring.actions.echo;

import com.example.wiring.valueentities.user.UserEntity;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.JavaTypedKalixClient;
import kalix.javasdk.impl.client.ServiceCall2;
import kalix.spring.KalixClient;
import kalix.spring.TypedKalixClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class ActionWithMetadata extends Action {

  private KalixClient kalixClient;

  public ActionWithMetadata(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @GetMapping("/action-with-meta/{key}/{value}")
  public Effect<Message> actionWithMeta(@PathVariable String key, @PathVariable String value) {
    var def = kalixClient.get("/return-meta/" + key, Message.class).withMetadata(Metadata.EMPTY.add(key, value)).execute();

    var typedClient = new TypedKalixClient(null);
    var javaTypedClient = new JavaTypedKalixClient(kalixClient);
    ServiceCall2<String, String, Object> stringStringObjectServiceCall2 = typedClient.ref2(UserEntity::createOrUpdateUser);
    ServiceCall2<String, String, String> stringStringStringServiceCall2 = javaTypedClient.ref2(UserEntity::createOrUpdateUser);
    return effects().asyncReply(def);
  }


  @GetMapping("/return-meta/{key}")
  public Effect<Message> returnMeta(@PathVariable String key) {
    var metaValue = actionContext().metadata().get(key).get();
    return effects().reply(new Message(metaValue));
  }
}
