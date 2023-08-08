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

package kalix.javasdk.valueentity;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("ve")
@RequestMapping("/ve")
public class TestValueEntityMigration extends ValueEntity<TestVEState2> {

//  @PostMapping("/create")
//  public Effect<String> create() {
//    return effects().updateState(new TestVEState1("test", 123)).thenReply("ok");
//  }

  @GetMapping
  public Effect<TestVEState2> get() {
    return effects().reply(currentState());
  }

}
