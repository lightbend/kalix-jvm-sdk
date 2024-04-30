/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl
import com.google.protobuf.Descriptors

/**
 * Service describes a component type in a way which makes it possible to deploy.
 */
trait Service {

  /**
   * @return
   *   a Protobuf ServiceDescriptor of its externally accessible gRPC API
   */
  def descriptor: Descriptors.ServiceDescriptor

  /**
   * @return
   *   a Protobuf FileDescriptor of any API's that need to be available either to API consumers (message types etc) or
   *   the backoffice API (state model etc).
   */
  def additionalDescriptors: Array[Descriptors.FileDescriptor]

  /**
   * @return
   *   the type of component represented by this service
   */
  def componentType: String

  /**
   * The service name used for by this service. By default, we use the descriptor name, but may be overwritten by user
   * definitions, for instance, viewId in the case of Views.
   */
  def serviceName: String = descriptor.getName

  /**
   * @return
   *   the options [[ComponentOptions]] or [[kalix.javasdk.EntityOptions]] used by this service
   */
  def componentOptions: Option[ComponentOptions]

  /**
   * @return
   *   a dictionary of service methods (Protobuf Descriptors.MethodDescriptor) classified by method name. The dictionary
   *   values represent a mapping of Protobuf Descriptors.MethodDescriptor with its input and output types (see
   *   [[kalix.javasdk.impl.ResolvedServiceMethod]])
   */
  def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]]
}
