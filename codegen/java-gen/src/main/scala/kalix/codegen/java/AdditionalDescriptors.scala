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

  /**
   * The file descriptor objects (outer classes) that [[collectServiceDescriptors]] refers to, for callers that render
   * them with imports taken into account.
   */
  def collectServiceDescriptorObjects(service: ModelBuilder.Service): Seq[ProtoMessageType] =
    (collectRelevantTypes(service.commandTypes, service.messageType)
      .collect { case pmt: ProtoMessageType => pmt }
      .flatMap(_.descriptorObject) ++ service.messageType.descriptorObject).toSeq.distinct
}
