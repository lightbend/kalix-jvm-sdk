/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;

import kalix.javasdk.Kalix;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import com.google.protobuf.Descriptors;

import java.util.Optional;

/**
 * Register a value based entity in {@link Kalix} using a <code>
 * ValueEntityProvider</code>. The concrete <code>ValueEntityProvider</code> is generated for the
 * specific entities defined in Protobuf, for example <code>CustomerEntityProvider</code>.
 */
public interface ValueEntityProvider<S, E extends ValueEntity<S>> {

  ValueEntityOptions options();

  Descriptors.ServiceDescriptor serviceDescriptor();

  String typeId();

  ValueEntityRouter<S, E> newRouter(ValueEntityContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();

  default Optional<MessageCodec> alternativeCodec() {
    return Optional.empty();
  }
}
