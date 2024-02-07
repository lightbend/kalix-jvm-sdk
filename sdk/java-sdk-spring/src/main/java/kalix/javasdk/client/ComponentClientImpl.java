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

package kalix.javasdk.client;

import kalix.javasdk.Metadata;
import kalix.spring.KalixClient;

import java.util.List;
import java.util.Optional;

/**
 * Utility to send requests to other Kalix components by composing a DeferredCall. To compose a call:
 * 1. select component type (and pass id if necessary)
 * 2. select component method, by using Java method reference operator (::)
 * 3. provide parameters (if required)
 *
 * <p>
 * Example of use on a cross-component call:
 * <pre>{@code
 * public Effect<String> createUser(String userId, String email, String name) {
 *   //validation here
 *   var defCall = componentClient.forValueEntity(userId).call(UserEntity::createUser).params(email, name);
 *   return effects().forward(defCall);
 * }
 * }</pre>
 */
public class ComponentClientImpl implements ComponentClient {

  private final KalixClient kalixClient;

  private Optional<Metadata> callMetadata = Optional.empty();

  public ComponentClientImpl(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public void setCallMetadata(Metadata callMetadata) {
    this.callMetadata = Optional.of(callMetadata);
  }

  public void clearCallMetadata() {
    this.callMetadata = Optional.empty();
  }

  @Override
  public ActionCallBuilder forAction() {
    return new ActionCallBuilder(kalixClient);
  }

  @Override
  public ValueEntityCallBuilder forValueEntity() {
    return new ValueEntityCallBuilder(kalixClient);
  }

  @Override
  public ValueEntityCallBuilder forValueEntity(String valueEntityId) {
    return new ValueEntityCallBuilder(kalixClient, valueEntityId);
  }

  @Override
  public ValueEntityCallBuilder forValueEntity(String... valueEntityIds) {
    return new ValueEntityCallBuilder(kalixClient, List.of(valueEntityIds));
  }

  @Override
  public EventSourcedEntityCallBuilder forEventSourcedEntity() {
    return new EventSourcedEntityCallBuilder(kalixClient);
  }

  @Override
  public EventSourcedEntityCallBuilder forEventSourcedEntity(String eventSourcedEntityId) {
    return new EventSourcedEntityCallBuilder(kalixClient, eventSourcedEntityId);
  }

  @Override
  public EventSourcedEntityCallBuilder forEventSourcedEntity(String... eventSourcedEntityIds) {
    return new EventSourcedEntityCallBuilder(kalixClient, List.of(eventSourcedEntityIds));
  }

  @Override
  public WorkflowCallBuilder forWorkflow() {
    return new WorkflowCallBuilder(kalixClient);
  }

  @Override
  public WorkflowCallBuilder forWorkflow(String workflowId) {
    return new WorkflowCallBuilder(kalixClient, workflowId);
  }

  @Override
  public WorkflowCallBuilder forWorkflow(String... workflowIds) {
    return new WorkflowCallBuilder(kalixClient, List.of(workflowIds));
  }

  @Override
  public ViewCallBuilder forView() {
    return new ViewCallBuilder(kalixClient);
  }

}