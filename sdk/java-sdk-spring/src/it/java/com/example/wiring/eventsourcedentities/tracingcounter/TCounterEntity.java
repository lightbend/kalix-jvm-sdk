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

package com.example.wiring.eventsourcedentities.tracingcounter;


import com.example.wiring.eventsourcedentities.counter.Counter;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("tcounter-entity")
@RequestMapping("/tcounter/{id}")
public class TCounterEntity extends EventSourcedEntity<TCounter, TCounterEvent> {

    Logger log = LoggerFactory.getLogger(TCounterEntity.class);

    private EventSourcedEntityContext context;

    public TCounterEntity(EventSourcedEntityContext context){
        this.context = context;
    }

    @Override
    public TCounter emptyState() {
        return new TCounter(0);
    }

    @PostMapping("/increase/{value}")
    public Effect<Integer> increase(@PathVariable Integer value){
        log.info("increasing [{}].", value);
        return effects().emitEvent(new TCounterEvent.ValueIncreased(value)).thenReply(c -> c.count());
    }

    @EventHandler
    public TCounter handleIncrease(TCounterEvent.ValueIncreased increase){
        return currentState().onValueIncrease(increase.value());
    }
}
