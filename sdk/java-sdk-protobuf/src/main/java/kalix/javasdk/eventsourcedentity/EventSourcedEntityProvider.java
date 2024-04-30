/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import kalix.javasdk.Kalix;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import com.google.protobuf.Descriptors;

import java.util.Optional;

/**
 * Register an event sourced entity in {@link Kalix} using a <code>
 * EventSourcedEntityProvider</code>. The concrete <code>EventSourcedEntityProvider</code> is
 * generated for the specific entities defined in Protobuf, for example <code>CustomerEntityProvider
 * </code>.
 */
public interface EventSourcedEntityProvider<S, E, ES extends EventSourcedEntity<S, E>> {

  EventSourcedEntityOptions options();

  Descriptors.ServiceDescriptor serviceDescriptor();

  String typeId();

  EventSourcedEntityRouter<S, E, ES> newRouter(EventSourcedEntityContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();

  default Optional<MessageCodec> alternativeCodec() {
    return Optional.empty();
  }
}
