/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.view

import kalix.scalasdk.valueentity.ValueEntityContext

/** A value entity. */
class ViewTckSourceEntity(context: ValueEntityContext) extends AbstractViewTckSourceEntity {
  override def emptyState: Ignore = Ignore.defaultInstance

}
