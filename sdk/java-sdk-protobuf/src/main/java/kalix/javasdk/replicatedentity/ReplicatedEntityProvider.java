/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.javasdk.Kalix;
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.replicatedentity.ReplicatedData;
import com.google.protobuf.Descriptors;

/**
 * Register a value based entity in {@link Kalix} using a <code>
 *  ReplicatedEntityProvider</code>. The concrete <code>ReplicatedEntityProvider</code> is generated
 * for the specific entities defined in Protobuf.
 */
public interface ReplicatedEntityProvider<D extends ReplicatedData, E extends ReplicatedEntity<D>> {

  ReplicatedEntityOptions options();

  Descriptors.ServiceDescriptor serviceDescriptor();

  String typeId();

  ReplicatedEntityRouter<D, E> newRouter(ReplicatedEntityContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();
}
