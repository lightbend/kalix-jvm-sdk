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

package kalix.codegen.java

import kalix.codegen.ModelBuilder
import kalix.codegen.ProtoMessageType
import kalix.codegen.SourceGeneratorUtils.collectRelevantTypes

object AdditionalDescriptors {

  def collectServiceDescriptors(service: ModelBuilder.Service): Seq[String] = {
    val relevantDescriptors =
      collectRelevantTypes(service.commandTypes, service.messageType)
        .collect { case pmt: ProtoMessageType =>
          s"${pmt.parent.javaOuterClassname}.getDescriptor()"
        }

    (relevantDescriptors :+ s"${service.messageType.parent.javaOuterClassname}.getDescriptor()").distinct.sorted
  }
}
