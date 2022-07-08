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

package kalix.springsdk.impl

import kalix.springsdk.annotations.Entity
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.RestServiceIntrospector

private[impl] final class EntityDescriptorFactory[T](val component: Class[T]) extends ComponentDescriptorFactory[T] {

  private val entityKeys: Seq[String] = component.getAnnotation(classOf[Entity]).entityKey()

  private def kalixMethods: Seq[KalixMethod] =
    RestServiceIntrospector.inspectService(component).methods.map { restMethod =>
      KalixMethod(restMethod, entityKeys = entityKeys)
    }

  override def buildDescriptor(nameGenerator: NameGenerator): ComponentDescriptor = {
    val serviceName = nameGenerator.getName(component.getSimpleName)
    kalixMethods.foldLeft(new ComponentDescriptor(serviceName, component.getPackageName, nameGenerator)) {
      (desc, method) =>
        desc.withMethod(method)
    }
  }
}
