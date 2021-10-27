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
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.impl.JavaDeferredCallAdapter
import com.akkaserverless.scalasdk.impl.MetadataConverters
import com.akkaserverless.scalasdk.impl.MetadataImpl
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }

import scala.jdk.FutureConverters._
import scala.concurrent.Future

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
  def apply[T, R](serviceCall: DeferredCall[T, R], synchronous: Boolean): SideEffect = ScalaSideEffectAdapter(
    javasdk.SideEffect.of(JavaDeferredCallAdapter(serviceCall), synchronous))

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall
   *   The service call to effect.
   * @return
   *   The side effect.
   */
  def apply[T, R](serviceCall: DeferredCall[T, R]): SideEffect = ScalaSideEffectAdapter(
    javasdk.SideEffect.of(JavaDeferredCallAdapter(serviceCall)))
}

trait SideEffect {

  /** The service call that is executed as this effect. */
  def serviceCall: DeferredCall[_ <: Any, _ <: Any]

  /** Whether this effect should be executed synchronously or not. */
  def synchronous: Boolean
}

private[scalasdk] final case class ScalaSideEffectAdapter(javasdkSideEffect: javasdk.SideEffect) extends SideEffect {
  override def serviceCall: DeferredCall[_, _] = ScalaDeferredCallAdapter(javasdkSideEffect.call())
  override def synchronous: Boolean = javasdkSideEffect.synchronous()
}

private[scalasdk] final case class ScalaDeferredCallAdapter[I, O](javasdkDeferredCall: javasdk.DeferredCall[I, O])
    extends DeferredCall[I, O] {
  override def message: I = javasdkDeferredCall.message
  override def metadata: Metadata =
    MetadataConverters.toScala(javasdkDeferredCall.metadata())

  def execute(): Future[O] = javasdkDeferredCall.execute().asScala
}
