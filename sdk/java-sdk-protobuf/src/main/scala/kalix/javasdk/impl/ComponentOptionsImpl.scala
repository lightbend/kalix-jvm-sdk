/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import java.util
import java.util.Collections

private[impl] final case class ComponentOptionsImpl(override val forwardHeaders: java.util.Set[String])
    extends ComponentOptions {

  override def withForwardHeaders(headers: util.Set[String]): ComponentOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)));
}
