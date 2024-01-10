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

package com.example.wiring.valueentities.user;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.valueentity.ValueEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@TypeId("compound-id-counter")
@Id({"id_part_1", "id_part_2"})
@RequestMapping("/compound-id-counter")
public class CompoundIdCounterEntity extends ValueEntity<Integer> {


  @Override
  public Integer emptyState() {
    return 0;
  }

  @PostMapping("/{id_part_1}/{id_part_2}/set/{value}")
  public Effect<String> set(@PathVariable Integer value) {
    return effects().updateState(value).thenReply("OK");
  }

  @GetMapping("/{id_part_1}/{id_part_2}")
  public Effect<Integer> get() {
    return effects().reply(currentState());
  }
}
