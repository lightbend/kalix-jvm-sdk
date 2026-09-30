/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.ModelBuilder
import kalix.codegen.ProtoMessageType
import kalix.codegen.SourceGeneratorUtils.collectRelevantTypes

object AdditionalDescriptors {

  def collectServiceDescriptors(service: ModelBuilder.Service): Seq[String] =
    collectServiceDescriptorObjects(service).map(outerClass => s"${outerClass.name}.getDescriptor()").distinct.sorted

  /**
   * The file descriptor objects (outer classes) of the service's own definition file and of the files of its command
   * types, for callers that render them with imports taken into account.
   */
  def collectServiceDescriptorObjects(service: ModelBuilder.Service): Seq[ProtoMessageType] =
    (collectRelevantTypes(service.commandTypes, service.messageType).flatMap(_.descriptorObject) ++
      service.messageType.descriptorObject).distinct
}
