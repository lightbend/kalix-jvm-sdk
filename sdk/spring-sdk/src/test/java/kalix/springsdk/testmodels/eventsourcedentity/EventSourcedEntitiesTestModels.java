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

package kalix.springsdk.testmodels.eventsourcedentity;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.Entity;
import kalix.springsdk.annotations.EventHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class EventSourcedEntitiesTestModels {

  @Entity(entityKey = "id", entityType = "counter")
  @RequestMapping("/eventsourced/{id}")
  public static class WellAnnotatedESEntity extends EventSourcedEntity<Integer> {

    @GetMapping("/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }

    @PostMapping("/changeInt/{number}")
    public Integer changeInteger(@PathVariable Integer number) {
      return number;
    }

    @EventHandler
    public Integer receiveStringEvent(String event) {
      return 0;
    }

    @EventHandler
    public Integer receivedIntegerEvent(Integer event) {
      return 0;
    }

    public Integer publicMethodSimilarSignature(Integer event) {
      return 0;
    }

    private Integer privateMethodSimilarSignature(Integer event) {
      return 0;
    }
  }

  @Entity(entityKey = "id", entityType = "counter")
  public static class ErrorDuplicatedEventsEntity extends EventSourcedEntity<Integer> {

    @EventHandler
    public Integer receiveStringEvent(String event) {
      return 0;
    }

    @EventHandler
    public Integer receivedIntegerEvent(Integer event) {
      return 0;
    }

    @EventHandler
    public Integer receivedIntegerEventDup(Integer event) {
      return 0;
    }
  }

  @Entity(entityKey = "id", entityType = "counter")
  public static class ErrorWrongSignaturesEntity extends EventSourcedEntity<Integer> {

    @EventHandler
    public String receivedIntegerEvent(Integer event) {
      return "0";
    }

    @EventHandler
    public Integer receivedIntegerEventAndString(Integer event, String s1) {
      return 0;
    }
  }
}
