/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import scala.reflect.ClassTag

import kalix.javasdk.eventsourcedentity.TestEventSourcedEntity

/**
 * Utility class to quickly print descriptors
 */
object DescriptorPrinter {

  def descriptorFor[T](implicit ev: ClassTag[T]): ComponentDescriptor =
    ComponentDescriptor.descriptorFor(ev.runtimeClass, new JsonMessageCodec)

  def main(args: Array[String]) = {
    val descriptor = descriptorFor[TestEventSourcedEntity]
    println(ProtoDescriptorRenderer.toString(descriptor.fileDescriptor))
  }
}
