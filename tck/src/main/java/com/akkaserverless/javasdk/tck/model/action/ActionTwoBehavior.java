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

package com.akkaserverless.javasdk.tck.model.action;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.tck.model.Action.OtherRequest;
import com.akkaserverless.tck.model.Action.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ActionTwoBehavior extends Action {
  public ActionTwoBehavior(ActionCreationContext creationContext) {}

  public CompletionStage<Reply<Response>> call(OtherRequest request) {
    return CompletableFuture.completedFuture(Reply.message(Response.getDefaultInstance()));
  }
}
