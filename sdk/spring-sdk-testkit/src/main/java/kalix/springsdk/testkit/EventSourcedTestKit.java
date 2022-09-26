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

package kalix.springsdk.testkit;

import kalix.javasdk.Metadata;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.impl.EventSourcedEntityEffectsRunner;
import kalix.javasdk.testkit.impl.TestKitEventSourcedEntityContext;
import kalix.springsdk.impl.eventsourcedentity.EventSourceEntityHandlers;
import kalix.springsdk.impl.eventsourcedentity.EventSourcedHandlersExtractor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * EventSourced Testkit for use in unit tests for EventSourced entities.
 *
 * <p>To test a EventSourced create a testkit instance by calling one of the available {@code
 * EventSourcedTestKit.of} methods. The returned testkit is stateful, and it holds internally the
 * state of the entity.
 *
 * <p>Use the {@code call} methods to interact with the testkit.
 */
public class EventSourcedTestKit<S, E extends EventSourcedEntity<S>>
    extends EventSourcedEntityEffectsRunner<S> {

  private final E entity;
  private final EventSourceEntityHandlers eventHandlers;

  private EventSourcedTestKit(E entity) {
    super(entity);
    this.entity = entity;
    eventHandlers = EventSourcedHandlersExtractor.handlersFrom(entity.getClass());
  }

  /**
   * Creates a new testkit instance from a EventSourcedEntity Supplier.
   *
   * <p>A default test entity id will be automatically provided.
   */
  public static <S, E extends EventSourcedEntity<S>> EventSourcedTestKit<S, E> of(
      Supplier<E> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Creates a new testkit instance from a function EventSourcedEntityContext to EventSourcedEntity.
   *
   * <p>A default test entity id will be automatically provided.
   */
  public static <S, E extends EventSourcedEntity<S>> EventSourcedTestKit<S, E> of(
      Function<EventSourcedEntityContext, E> entityFactory) {
    return of("testkit-entity-id", entityFactory);
  }

  /**
   * Creates a new testkit instance from a user defined entity id and an EventSourcedEntity
   * Supplier.
   */
  public static <S, E extends EventSourcedEntity<S>> EventSourcedTestKit<S, E> of(
      String entityId, Supplier<E> entityFactory) {
    return of(entityId, ctx -> entityFactory.get());
  }

  /**
   * Creates a new testkit instance from a user defined entity id and a function
   * EventSourcedEntityContext to EventSourcedEntity.
   */
  public static <S, E extends EventSourcedEntity<S>> EventSourcedTestKit<S, E> of(
      String entityId, Function<EventSourcedEntityContext, E> entityFactory) {
    EventSourcedEntityContext context = new TestKitEventSourcedEntityContext(entityId);
    return new EventSourcedTestKit<>(entityFactory.apply(context));
  }

  /**
   * The call method can be used to simulate a call to the EventSourcedEntity. The passed java
   * lambda should return an EventSourcedEntity.Effect. The Effect is interpreted into an
   * EventSourcedResult that can be used in test assertions.
   *
   * @param func A function from EventSourcedEntity to EventSourcedEntity.Effect.
   * @return a EventSourcedResult
   * @param <R> The type of reply that is expected from invoking a command handler
   */
  public <R> EventSourcedResult<R> call(Function<E, EventSourcedEntity.Effect<R>> func) {
    return interpretEffects(() -> func.apply(entity), Metadata.EMPTY);
  }

  @Override
  protected final S handleEvent(S state, Object event) {
    try {
      Method method = eventHandlers.handlers().apply(event.getClass());
      return (S) method.invoke(entity, event);
    } catch (NoSuchElementException e) {
      throw new RuntimeException(
          "Couldn't find a valid event handler for event type '"
              + event.getClass().getName()
              + "'");
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
