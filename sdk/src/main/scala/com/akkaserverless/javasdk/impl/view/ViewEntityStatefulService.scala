/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.view

import com.akkaserverless.javasdk.Service
import com.akkaserverless.javasdk.impl.ResolvedServiceMethod
import com.akkaserverless.protocol.view_entity.ViewEntity
import com.google.protobuf.Descriptors

class ViewEntityStatefulService(override val descriptor: Descriptors.ServiceDescriptor, viewId: String)
    extends Service {
  override def componentType: String = ViewEntity.name
  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] = None
  override def entityType: String = viewId
}
