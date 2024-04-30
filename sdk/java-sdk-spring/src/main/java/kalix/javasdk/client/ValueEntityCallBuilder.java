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
import kalix.javasdk.Metadata;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.spring.impl.KalixClient;

import java.util.List;
import java.util.Optional;

public class ValueEntityCallBuilder {

  private final KalixClient kalixClient;
  private final Optional<Metadata> callMetadata;
  private final List<String> entityIds;


  public ValueEntityCallBuilder(KalixClient kalixClient, Optional<Metadata> callMetadata, List<String> entityIds) {
    this.kalixClient = kalixClient;
    this.callMetadata = callMetadata;
    this.entityIds = entityIds;
  }


  public ValueEntityCallBuilder(KalixClient kalixClient,  Optional<Metadata> callMetadata, String entityId) {
    this(kalixClient, callMetadata, List.of(entityId));
  }

  public ValueEntityCallBuilder(KalixClient kalixClient,  Optional<Metadata> callMetadata) {
    this(kalixClient, callMetadata, List.of());
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, R> DeferredCall<Any, R> call(Function<T, ValueEntity.Effect<R>> methodRef) {
    DeferredCall<Any, R> result = ComponentCall.noParams(kalixClient, methodRef, entityIds);
    return result.withMetadata(ComponentCall.addTracing(result.metadata(), callMetadata));
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, R> ComponentCall<A1, R> call(Function2<T, A1, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, R> ComponentCall2<A1, A2, R> call(Function3<T, A1, A2, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall2<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, R> ComponentCall3<A1, A2, A3, R> call(Function4<T, A1, A2, A3, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall3<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, R> ComponentCall4<A1, A2, A3, A4, R> call(Function5<T, A1, A2, A3, A4, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall4<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, R> ComponentCall5<A1, A2, A3, A4, A5, R> call(Function6<T, A1, A2, A3, A4, A5, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall5<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, R> ComponentCall6<A1, A2, A3, A4, A5, A6, R> call(Function7<T, A1, A2, A3, A4, A5, A6, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall6<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, R> ComponentCall7<A1, A2, A3, A4, A5, A6, A7, R> call(Function8<T, A1, A2, A3, A4, A5, A6, A7, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall7<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, R> ComponentCall8<A1, A2, A3, A4, A5, A6, A7, A8, R> call(Function9<T, A1, A2, A3, A4, A5, A6, A7, A8, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall8<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, R> ComponentCall9<A1, A2, A3, A4, A5, A6, A7, A8, A9, R> call(Function10<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall9<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> ComponentCall10<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> call(Function11<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall10<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> ComponentCall11<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> call(Function12<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall11<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> ComponentCall12<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> call(Function13<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall12<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> ComponentCall13<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> call(Function14<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall13<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> ComponentCall14<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> call(Function15<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall14<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> ComponentCall15<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> call(Function16<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall15<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> ComponentCall16<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> call(Function17<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall16<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> ComponentCall17<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> call(Function18<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall17<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> ComponentCall18<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> call(Function19<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall18<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> ComponentCall19<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> call(Function20<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall19<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> ComponentCall20<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> call(Function21<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall20<>(kalixClient, methodRef, entityIds, callMetadata);
  }

  /**
   * Pass in a Value Entity method reference annotated as a REST endpoint, e.g. <code>UserEntity::create</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> ComponentCall21<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> call(Function22<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall21<>(kalixClient, methodRef, entityIds, callMetadata);
  }
}
