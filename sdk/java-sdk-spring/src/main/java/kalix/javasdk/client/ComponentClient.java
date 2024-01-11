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

import kalix.spring.KalixClient;

import java.util.List;

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
public class ComponentClient {

  private final KalixClient kalixClient;

  public ComponentClient(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  /**
   * Select Action as a call target component.
   */
  public ActionCallBuilder forAction() {
    return new ActionCallBuilder(kalixClient);
  }

  /**
   * Select ValueEntity as a call target component.
   * <p>
   * For calling methods annotated with @{@link kalix.javasdk.annotations.GenerateId}
   */
  public ValueEntityCallBuilder forValueEntity() {
    return new ValueEntityCallBuilder(kalixClient);
  }

  /**
   * Select ValueEntity as a call target component.
   *
   * @param valueEntityId - value entity id used to create a call.
   */
  public ValueEntityCallBuilder forValueEntity(String valueEntityId) {
    return new ValueEntityCallBuilder(kalixClient, valueEntityId);
  }

  /**
   * Select ValueEntity as a call target component.
   *
   * @param valueEntityIds - compound entity ids used to create a call.
   */
  public ValueEntityCallBuilder forValueEntity(String... valueEntityIds) {
    return new ValueEntityCallBuilder(kalixClient, List.of(valueEntityIds));
  }

  /**
   * Select EventSourcedEntity as a call target component.
   * <p>
   * For calling methods annotated with @{@link kalix.javasdk.annotations.GenerateId}
   */
  public EventSourcedEntityCallBuilder forEventSourcedEntity() {
    return new EventSourcedEntityCallBuilder(kalixClient);
  }

  /**
   * Select EventSourcedEntity as a call target component.
   *
   * @param eventSourcedEntityId - event sourced entity id used to create a call.
   */
  public EventSourcedEntityCallBuilder forEventSourcedEntity(String eventSourcedEntityId) {
    return new EventSourcedEntityCallBuilder(kalixClient, eventSourcedEntityId);
  }

  /**
   * Select EventSourcedEntity as a call target component.
   *
   * @param eventSourcedEntityIds - compound entity ids used to create a call.
   */
  public EventSourcedEntityCallBuilder forEventSourcedEntity(String... eventSourcedEntityIds) {
    return new EventSourcedEntityCallBuilder(kalixClient, List.of(eventSourcedEntityIds));
  }

  /**
   * Select Workflow as a call target component.
   * <p>
   * For calling methods annotated with @{@link kalix.javasdk.annotations.GenerateId}
   */
  public WorkflowCallBuilder forWorkflow() {
    return new WorkflowCallBuilder(kalixClient);
  }

  /**
   * Select Workflow as a call target component.
   *
   * @param workflowId - workflow id used to create a call.
   */
  public WorkflowCallBuilder forWorkflow(String workflowId) {
    return new WorkflowCallBuilder(kalixClient, workflowId);
  }

  /**
   * Select Workflow as a call target component.
   *
   * @param workflowIds - compound workflow ids used to create a call.
   */
  public WorkflowCallBuilder forWorkflow(String... workflowIds) {
    return new WorkflowCallBuilder(kalixClient, List.of(workflowIds));
  }

  /**
   * Select View as a call target component.
   */
  public ViewCallBuilder forView() {
    return new ViewCallBuilder(kalixClient);
  }

}
