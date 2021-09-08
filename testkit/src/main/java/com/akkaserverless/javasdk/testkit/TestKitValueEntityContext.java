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

package com.akkaserverless.javasdk.testkit;

import com.akkaserverless.javasdk.ServiceCallFactory;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;

public final class TestKitValueEntityContext implements ValueEntityContext {

  private final String entityId;

  public TestKitValueEntityContext(String entityId) {
    this.entityId = entityId;
  }

  @Override
  public String entityId() {
    return entityId;
  }

  @Override
  public ServiceCallFactory serviceCallFactory() {
    throw new UnsupportedOperationException(
        "Accessing service call factory in testkit not supported yet");
  }
}
