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

import kalix.javasdk.HttpResponse;
import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.GetMapping;

public class ActionWithHttpResponse extends Action {

  @GetMapping("/text-body")
  public Effect<HttpResponse> textBody() {
    return effects().reply(HttpResponse.ok("test"));
  }

  @GetMapping("/empty-text-body")
  public Effect<HttpResponse> emptyCreated() {
    return effects().reply(HttpResponse.created());
  }

  @GetMapping("/json-string-body")
  public Effect<HttpResponse> jsonStringBody() {
    return effects().reply(HttpResponse.of(StatusCode.Success.OK, "application/json", "{\"text\": \"123\"}".getBytes()));
  }

  @GetMapping("/json-body")
  public Effect<HttpResponse> jsonBody() {
    return effects().reply(HttpResponse.ok(new Message("321")));
  }

}
