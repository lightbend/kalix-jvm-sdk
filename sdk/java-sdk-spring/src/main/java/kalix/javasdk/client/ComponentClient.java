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

public interface ComponentClient {
    /**
     * Select Action as a call target component.
     */
    ActionCallBuilder forAction();

    /**
     * Select ValueEntity as a call target component.
     * <p>
     * For calling methods annotated with @{@link kalix.javasdk.annotations.GenerateId}
     */
    ValueEntityCallBuilder forValueEntity();

    /**
     * Select ValueEntity as a call target component.
     *
     * @param valueEntityId - value entity id used to create a call.
     */
    ValueEntityCallBuilder forValueEntity(String valueEntityId);

    /**
     * Select ValueEntity as a call target component.
     *
     * @param valueEntityIds - compound entity ids used to create a call.
     */
    ValueEntityCallBuilder forValueEntity(String... valueEntityIds);

    /**
     * Select EventSourcedEntity as a call target component.
     * <p>
     * For calling methods annotated with @{@link kalix.javasdk.annotations.GenerateId}
     */
    EventSourcedEntityCallBuilder forEventSourcedEntity();

    /**
     * Select EventSourcedEntity as a call target component.
     *
     * @param eventSourcedEntityId - event sourced entity id used to create a call.
     */
    EventSourcedEntityCallBuilder forEventSourcedEntity(String eventSourcedEntityId);

    /**
     * Select EventSourcedEntity as a call target component.
     *
     * @param eventSourcedEntityIds - compound entity ids used to create a call.
     */
    EventSourcedEntityCallBuilder forEventSourcedEntity(String... eventSourcedEntityIds);

    /**
     * Select Workflow as a call target component.
     * <p>
     * For calling methods annotated with @{@link kalix.javasdk.annotations.GenerateId}
     */
    WorkflowCallBuilder forWorkflow();

    /**
     * Select Workflow as a call target component.
     *
     * @param workflowId - workflow id used to create a call.
     */
    WorkflowCallBuilder forWorkflow(String workflowId);

    /**
     * Select Workflow as a call target component.
     *
     * @param workflowIds - compound workflow ids used to create a call.
     */
    WorkflowCallBuilder forWorkflow(String... workflowIds);

    /**
     * Select View as a call target component.
     */
    ViewCallBuilder forView();
}
