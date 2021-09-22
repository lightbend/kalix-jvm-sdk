/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.scalasdk

import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.impl.JavaServiceCallAdapter
import com.akkaserverless.scalasdk.impl.MetadataImpl
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }

/* A side effect. */
object SideEffect {

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall
   *   The service call to effect.
   * @param synchronous
   *   Whether this effect should be executed synchronously.
   * @return
   *   The side effect.
   */
  def apply(serviceCall: ServiceCall, synchronous: Boolean): SideEffect = ScalaSideEffectAdapter(
    javasdk.SideEffect.of(JavaServiceCallAdapter(serviceCall), synchronous))

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall
   *   The service call to effect.
   * @return
   *   The side effect.
   */
  def apply(serviceCall: ServiceCall): SideEffect = ScalaSideEffectAdapter(
    javasdk.SideEffect.of(JavaServiceCallAdapter(serviceCall)))
}

trait SideEffect {

  /** The service call that is executed as this effect. */
  def serviceCall: ServiceCall

  /** Whether this effect should be executed synchronously or not. */
  def synchronous: Boolean
}

private[scalasdk] case class ScalaSideEffectAdapter(javasdkSideEffect: javasdk.SideEffect) extends SideEffect {
  override def serviceCall: ServiceCall = ScalaServiceCallAdapter(javasdkSideEffect.serviceCall())
  override def synchronous: Boolean = javasdkSideEffect.synchronous()
}

private[scalasdk] case class ScalaServiceCallAdapter(javasdkServiceCall: javasdk.ServiceCall) extends ServiceCall {
  override def ref: ServiceCallRef[_] = ScalaServiceCallRefAdapter(javasdkServiceCall.ref)
  override def message: ScalaPbAny = ScalaPbAny.fromJavaProto(javasdkServiceCall.message)
  override def metadata: Metadata = {
    // FIXME can we get rid of this cast?
    new MetadataImpl(javasdkServiceCall.metadata().asInstanceOf[com.akkaserverless.javasdk.impl.MetadataImpl])
  }
}

private[scalasdk] case class ScalaServiceCallRefAdapter[T](javasdkServiceCallRef: javasdk.ServiceCallRef[T])
    extends ServiceCallRef[T] {
  def method: Descriptors.MethodDescriptor = javasdkServiceCallRef.method()

  def createCall(message: T, metadata: Metadata): ServiceCall = {
    ScalaServiceCallAdapter(
      javasdkServiceCallRef
        .createCall(message, metadata.impl))
  }
}
