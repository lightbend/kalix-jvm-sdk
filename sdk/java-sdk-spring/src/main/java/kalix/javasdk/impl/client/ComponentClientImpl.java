/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.client;

import kalix.javasdk.Metadata;
import kalix.javasdk.client.*;
import kalix.spring.impl.KalixClient;

import java.util.List;
import java.util.Optional;

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
    return new ActionCallBuilder(kalixClient, callMetadata);
  }

  @Override
  public ValueEntityCallBuilder forValueEntity() {
    return new ValueEntityCallBuilder(kalixClient, callMetadata);
  }

  @Override
  public ValueEntityCallBuilder forValueEntity(String valueEntityId) {
    return new ValueEntityCallBuilder(kalixClient, callMetadata, valueEntityId);
  }

  @Override
  public ValueEntityCallBuilder forValueEntity(String... valueEntityIds) {
    return new ValueEntityCallBuilder(kalixClient, callMetadata, List.of(valueEntityIds));
  }

  @Override
  public EventSourcedEntityCallBuilder forEventSourcedEntity() {
    return new EventSourcedEntityCallBuilder(kalixClient, callMetadata);
  }

  @Override
  public EventSourcedEntityCallBuilder forEventSourcedEntity(String eventSourcedEntityId) {
    return new EventSourcedEntityCallBuilder(kalixClient, callMetadata, eventSourcedEntityId);
  }

  @Override
  public EventSourcedEntityCallBuilder forEventSourcedEntity(String... eventSourcedEntityIds) {
    return new EventSourcedEntityCallBuilder(kalixClient, callMetadata, List.of(eventSourcedEntityIds));
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
    return new ViewCallBuilder(kalixClient, callMetadata);
  }

}
