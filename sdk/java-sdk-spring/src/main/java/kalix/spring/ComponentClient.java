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

package kalix.spring;

public class ComponentClient {

  private final KalixClient kalixClient;

  public ComponentClient(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public ActionCallBuilder forAction() {
    return new ActionCallBuilder(kalixClient);
  }

  public ValueEntityCallBuilder forValueEntity() {
    return new ValueEntityCallBuilder(kalixClient);
  }

  public ValueEntityCallBuilder forValueEntity(String entityId) {
    return new ValueEntityCallBuilder(kalixClient, entityId);
  }

  public EventSourcedEntityCallBuilder forEventSourcedEntity() {
    return new EventSourcedEntityCallBuilder(kalixClient);
  }

  public EventSourcedEntityCallBuilder forEventSourcedEntity(String entityId) {
    return new EventSourcedEntityCallBuilder(kalixClient, entityId);
  }
}
