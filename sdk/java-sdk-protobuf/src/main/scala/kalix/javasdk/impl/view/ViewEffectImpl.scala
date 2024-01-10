/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.impl.view

import kalix.javasdk.view.View

object ViewUpdateEffectImpl {
  sealed trait PrimaryUpdateEffect[S] extends View.UpdateEffect[S]
  case class Update[S](state: S) extends PrimaryUpdateEffect[S]
  case object Delete extends PrimaryUpdateEffect[Any]
  case object Ignore extends PrimaryUpdateEffect[Any]
  case class Error[T](description: String) extends PrimaryUpdateEffect[T]

  private val _builder = new View.UpdateEffect.Builder[Any] {
    override def updateState(newState: Any): View.UpdateEffect[Any] = Update(newState)
    override def deleteState(): View.UpdateEffect[Any] = Delete.asInstanceOf[PrimaryUpdateEffect[Any]]
    override def ignore(): View.UpdateEffect[Any] = Ignore.asInstanceOf[PrimaryUpdateEffect[Any]]
    override def error(description: String): View.UpdateEffect[Any] = Error(description)
  }
  def builder[S](): View.UpdateEffect.Builder[S] =
    _builder.asInstanceOf[View.UpdateEffect.Builder[S]]
}
