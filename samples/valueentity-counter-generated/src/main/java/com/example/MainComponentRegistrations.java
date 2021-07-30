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

package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.domain.Counter;
import com.example.domain.CounterDomain;
import com.google.protobuf.EmptyProto;

public final class MainComponentRegistrations {
    
    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        return akkaServerless
                .registerValueEntity(
                    Counter.class,
                    CounterApi.getDescriptor().findServiceByName("CounterService"),
                    CounterDomain.getDescriptor(),
                    EmptyProto.getDescriptor()
                );
    }
}