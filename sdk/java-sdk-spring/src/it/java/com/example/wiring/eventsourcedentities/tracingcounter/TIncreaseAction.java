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

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TIncreaseAction extends Action {

    Logger log = LoggerFactory.getLogger(TIncreaseAction.class);

    private ActionCreationContext actionCreationContext;

    public TIncreaseAction(ActionCreationContext actionCreationContext){
        this.actionCreationContext = actionCreationContext;
    }

    @Subscribe.EventSourcedEntity(value = TCounterEntity.class)
    public Effect<Integer> printIncrease(TCounterEvent.ValueIncreased increase){
        log.info("increasing [{}].", increase);
        return effects().reply(increase.value());
    }



}
