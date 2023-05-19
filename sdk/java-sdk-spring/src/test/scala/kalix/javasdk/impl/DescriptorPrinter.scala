package kalix.javasdk.impl

import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForTopicSubscription

import scala.reflect.ClassTag

/**
 * Utility class to quickly print descriptors
 */
object DescriptorPrinter {

  def descriptorFor[T](implicit ev: ClassTag[T]): ComponentDescriptor =
    ComponentDescriptor.descriptorFor(ev.runtimeClass, new JsonMessageCodec)

  def main(args: Array[String]) = {
    val descriptor = descriptorFor[MissingTopicForTopicSubscription]
    println(ProtoDescriptorRenderer.toString(descriptor.fileDescriptor))
  }
}
