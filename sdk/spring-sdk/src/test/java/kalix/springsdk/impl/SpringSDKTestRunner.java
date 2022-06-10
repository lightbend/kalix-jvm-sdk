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

package kalix.springsdk.impl;

import kalix.javasdk.Kalix;
import kalix.springsdk.action.EchoAction;
import kalix.springsdk.action.ReflectiveActionProvider;
import kalix.springsdk.valueentity.ReflectiveValueEntityProvider;
import kalix.springsdk.valueentity.UserEntity;

import java.util.concurrent.ExecutionException;

public class SpringSDKTestRunner {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    Kalix kalix = new Kalix();
    kalix
        .register(ReflectiveActionProvider.of(EchoAction.class, __ -> new EchoAction()))
        .register(
            ReflectiveValueEntityProvider.of(
                "user-entity", // TODO: we should have this in some type level annotation, ie:
                // @KalixComponent(name)
                UserEntity.class,
                __ -> new UserEntity()));
    kalix.start().toCompletableFuture().get();
  }
}
