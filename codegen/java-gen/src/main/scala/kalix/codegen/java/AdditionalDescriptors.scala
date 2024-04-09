/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
