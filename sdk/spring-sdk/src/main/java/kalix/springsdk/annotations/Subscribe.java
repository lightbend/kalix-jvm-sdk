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

package kalix.springsdk.annotations;

import java.lang.annotation.*;

/**
 * Annotation for providing ways to subscribe to different streams of information: value entities,
 * event-sourced entities or topic.
 */
public @interface Subscribe {

  /**
   * Annotation for subscribing to updates from a Value Entity. It can be used both at type and
   * method levels. When used at type level, it means the `View` will not be transforming state.
   * When used at method level, it gives the ability to transform the updates into a different state
   * for the view and the underlying method must be declared to receive one or two parameters:
   *
   * <ul>
   *   <li>when one parameter is passed, the single parameter will be considered the event type such
   *       method will handle;
   *   <li>when two parameters are passed, the first one will be considered the view state and the
   *       second one the event type.
   * </ul>
   */
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface ValueEntity {
    /**
     * Assign the class type of the entity one intends to subscribe to, which must extend {@link
     * kalix.javasdk.valueentity.ValueEntity ValueEntity}.
     */
    Class<? extends kalix.javasdk.valueentity.ValueEntity<?>> value();

    /**
     * Annotation used in the scope of a view or action for marking methods that should be invoked when an entity has been deleted.
     * Currently supported only with the {@link kalix.springsdk.annotations.Subscribe.ValueEntity} subscriptions.
     * If the method is in a view the return type should be the {@link kalix.javasdk.view.View.UpdateEffect} type, for actions
     * the returned message type can be an arbitrary message, for example for publishing to eventing out on delete.
     *
     * <p><b>Note: </b>annotated method cannot have any parameters.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface HandleDeletes {
      /**
       * Assign the class type of the entity one intends to subscribe to, which must extend {@link
       * kalix.javasdk.valueentity.ValueEntity ValueEntity}.
       */
      Class<? extends kalix.javasdk.valueentity.ValueEntity<?>> value();
    }
  }

  /**
   * Annotation for subscribing to updates from an Event-sourced Entity. It can be used only at type
   * level. The underlying method must be declared to receive one or two parameters:
   *
   * <ul>
   *   <li>when one parameter is passed, the single parameter will be considered the event type such
   *       method will handle;
   *   <li>when two parameters are passed, the first one will be considered the view state and the
   *       second one the event type.
   * </ul>
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface EventSourcedEntity {
    /**
     * Assign the class type of the entity one intends to subscribe to, which must extend {@link
     * kalix.javasdk.eventsourcedentity.EventSourcedEntity EventSourcedEntity}.
     */
    Class<? extends kalix.javasdk.eventsourcedentity.EventSourcedEntity<?>> value();

    /** This option is only available for classes. Using it in a method has no effect.
     * If there is no method in the class whose input type matches the event type:
     *   if ignoreUnknown is true the event is discarded.
     *   if false, an Exception is raised.
     * */
    boolean ignoreUnkown() default false;
  }

  /** Annotation for subscribing to messages from a topic (i.e PubSub or Kafka topic). */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Topic {
    /** Assign the name of the topic to consume the stream from. */
    String value();

    /** Assign the consumer group name to be used on the broker. */
    String consumerGroup() default "";

    /**
     * This option is only available for classes. Using it in a method has no effect.
     * If there is no method in the class whose input type matches the event type:
     *   if ignoreUnknown is true the event is discarded.
     *   if false, an Exception is raised.
     **/
    boolean ignoreUnknown() default false;
  }
}
