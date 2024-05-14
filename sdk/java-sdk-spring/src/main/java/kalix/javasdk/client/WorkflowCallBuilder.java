/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.client;

import akka.japi.function.Function;
import akka.japi.function.Function10;
import akka.japi.function.Function11;
import akka.japi.function.Function12;
import akka.japi.function.Function13;
import akka.japi.function.Function14;
import akka.japi.function.Function15;
import akka.japi.function.Function16;
import akka.japi.function.Function17;
import akka.japi.function.Function18;
import akka.japi.function.Function19;
import akka.japi.function.Function2;
import akka.japi.function.Function20;
import akka.japi.function.Function21;
import akka.japi.function.Function22;
import akka.japi.function.Function3;
import akka.japi.function.Function4;
import akka.japi.function.Function5;
import akka.japi.function.Function6;
import akka.japi.function.Function7;
import akka.japi.function.Function8;
import akka.japi.function.Function9;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.workflow.Workflow;
import kalix.spring.impl.KalixClient;

import java.util.List;
import java.util.Optional;

public class WorkflowCallBuilder {

  private final KalixClient kalixClient;
  private final List<String> workflowIds;

  public WorkflowCallBuilder(KalixClient kalixClient, List<String> workflowIds) {
    this.kalixClient = kalixClient;
    this.workflowIds = workflowIds;
  }

  public WorkflowCallBuilder(KalixClient kalixClient, String workflowId) {
    this(kalixClient, List.of(workflowId));
  }

  public WorkflowCallBuilder(KalixClient kalixClient) {
    this(kalixClient, List.of());
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, R> DeferredCall<Any, R> call(Function<T, Workflow.Effect<R>> methodRef) {
    return ComponentCall.noParams(kalixClient, methodRef, workflowIds, Optional.empty());
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, R> ComponentCall<A1, R> call(Function2<T, A1, Workflow.Effect<R>> methodRef) {
    return new ComponentCall<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, R> ComponentCall2<A1, A2, R> call(Function3<T, A1, A2, Workflow.Effect<R>> methodRef) {
    return new ComponentCall2<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, R> ComponentCall3<A1, A2, A3, R> call(Function4<T, A1, A2, A3, Workflow.Effect<R>> methodRef) {
    return new ComponentCall3<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, R> ComponentCall4<A1, A2, A3, A4, R> call(Function5<T, A1, A2, A3, A4, Workflow.Effect<R>> methodRef) {
    return new ComponentCall4<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, R> ComponentCall5<A1, A2, A3, A4, A5, R> call(Function6<T, A1, A2, A3, A4, A5, Workflow.Effect<R>> methodRef) {
    return new ComponentCall5<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, R> ComponentCall6<A1, A2, A3, A4, A5, A6, R> call(Function7<T, A1, A2, A3, A4, A5, A6, Workflow.Effect<R>> methodRef) {
    return new ComponentCall6<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, R> ComponentCall7<A1, A2, A3, A4, A5, A6, A7, R> call(Function8<T, A1, A2, A3, A4, A5, A6, A7, Workflow.Effect<R>> methodRef) {
    return new ComponentCall7<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, R> ComponentCall8<A1, A2, A3, A4, A5, A6, A7, A8, R> call(Function9<T, A1, A2, A3, A4, A5, A6, A7, A8, Workflow.Effect<R>> methodRef) {
    return new ComponentCall8<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, R> ComponentCall9<A1, A2, A3, A4, A5, A6, A7, A8, A9, R> call(Function10<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, Workflow.Effect<R>> methodRef) {
    return new ComponentCall9<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> ComponentCall10<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> call(Function11<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, Workflow.Effect<R>> methodRef) {
    return new ComponentCall10<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> ComponentCall11<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> call(Function12<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, Workflow.Effect<R>> methodRef) {
    return new ComponentCall11<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> ComponentCall12<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> call(Function13<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, Workflow.Effect<R>> methodRef) {
    return new ComponentCall12<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> ComponentCall13<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> call(Function14<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, Workflow.Effect<R>> methodRef) {
    return new ComponentCall13<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> ComponentCall14<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> call(Function15<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, Workflow.Effect<R>> methodRef) {
    return new ComponentCall14<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> ComponentCall15<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> call(Function16<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, Workflow.Effect<R>> methodRef) {
    return new ComponentCall15<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> ComponentCall16<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> call(Function17<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, Workflow.Effect<R>> methodRef) {
    return new ComponentCall16<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> ComponentCall17<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> call(Function18<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, Workflow.Effect<R>> methodRef) {
    return new ComponentCall17<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> ComponentCall18<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> call(Function19<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, Workflow.Effect<R>> methodRef) {
    return new ComponentCall18<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> ComponentCall19<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> call(Function20<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, Workflow.Effect<R>> methodRef) {
    return new ComponentCall19<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> ComponentCall20<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> call(Function21<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, Workflow.Effect<R>> methodRef) {
    return new ComponentCall20<>(kalixClient, methodRef, workflowIds);
  }

  /**
   * Pass in a Workflow method reference annotated as a REST endpoint, e.g. <code>MyWorkflow::start</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> ComponentCall21<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> call(Function22<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, Workflow.Effect<R>> methodRef) {
    return new ComponentCall21<>(kalixClient, methodRef, workflowIds);
  }
}
