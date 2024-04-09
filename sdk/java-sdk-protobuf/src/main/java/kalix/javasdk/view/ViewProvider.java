/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.view.ViewUpdateRouter;

import java.util.Optional;

public interface ViewProvider {

  Descriptors.ServiceDescriptor serviceDescriptor();

  String viewId();

  ViewOptions options();

  ViewUpdateRouter newRouter(ViewCreationContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();

  default Optional<MessageCodec> alternativeCodec() {
    return Optional.empty();
  }
}
