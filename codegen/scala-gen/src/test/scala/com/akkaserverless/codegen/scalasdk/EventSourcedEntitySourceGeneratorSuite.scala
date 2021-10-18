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

package com.akkaserverless.codegen.scalasdk

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.lightbend.akkasls.codegen.{ PackageNaming, TestData }
import protocgen.CodeGenRequest
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class EventSourcedEntitySourceGeneratorSuite extends munit.FunSuite {
  import com.akkaserverless.codegen.scalasdk.impl.EventSourcedEntitySourceGenerator._

  private val testData = TestData.scalaStyle

  test("it can generate an event sourced entity implementation skeleton") {
    val file =
      generateImplementationSkeleton(testData.eventSourcedEntity(), testData.simpleEntityService())
    assertEquals(
      file.content,
      s"""package com.example.service.domain
         |
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
         |import com.example.service
         |import com.external.Empty
         |
         |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
         |//
         |// As long as this file exists it will not be overwritten: you can maintain it yourself,
         |// or delete it so it is regenerated as needed.
         |
         |/** An event sourced entity. */
         |class MyEntity(context: EventSourcedEntityContext) extends AbstractMyEntity {
         |  override def emptyState: MyState =
         |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")
         |
         |  override def set(currentState: MyState, setValue: service.SetValue): EventSourcedEntity.Effect[Empty] =
         |    effects.error("The command handler for `Set` is not implemented, yet")
         |
         |  override def get(currentState: MyState, getValue: service.GetValue): EventSourcedEntity.Effect[service.MyState] =
         |    effects.error("The command handler for `Get` is not implemented, yet")
         |
         |  override def setEvent(currentState: MyState, setEvent: SetEvent): MyState =
         |    throw new RuntimeException("The event handler for `SetEvent` is not implemented, yet")
         |
         |}
         |""".stripMargin)
  }
  test("it can generate an abstract event sourced entity implementation") {
    val str = abstractEntity(testData.eventSourcedEntity(), testData.simpleEntityService()).content
    assertNoDiff(
      str,
      s"""|package com.example.service.domain
          |
          |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
          |import com.example.service
          |import com.external.Empty
          |
          |// This code is managed by Akka Serverless tooling.
          |// It will be re-generated to reflect any changes to your protobuf definitions.
          |// DO NOT EDIT
          |
          |/** An event sourced entity. */
          |abstract class AbstractMyEntity extends EventSourcedEntity[MyState] {
          |
          |  def set(currentState: MyState, setValue: service.SetValue): EventSourcedEntity.Effect[Empty]
          |
          |  def get(currentState: MyState, getValue: service.GetValue): EventSourcedEntity.Effect[service.MyState]
          |
          |  def setEvent(currentState: MyState, setEvent: SetEvent): MyState
          |}
          |
      """.stripMargin)
  }

  test("it can generate an event sourced entity handler implementation") {
    val str = handler(testData.eventSourcedEntity(), testData.simpleEntityService()).content
    assertNoDiff(
      str,
      s"""|package com.example.service.domain
          |
          |import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandHandlerNotFound
          |import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.EventHandlerNotFound
          |import com.akkaserverless.scalasdk.eventsourcedentity.CommandContext
          |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
          |import com.akkaserverless.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter
          |import com.example.service
          |
          |// This code is managed by Akka Serverless tooling.
          |// It will be re-generated to reflect any changes to your protobuf definitions.
          |// DO NOT EDIT
          |
          |/**
          | * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
          | * and the command handler methods in the <code>Counter</code> class.
          | */
          |class MyEntityRouter(entity: MyEntity) extends EventSourcedEntityRouter[MyState, MyEntity](entity) {
          |  def handleCommand(commandName: String, state: MyState, command: Any, context: CommandContext): EventSourcedEntity.Effect[_] = {
          |    commandName match {
          |      case "Set" =>
          |        entity.set(state, command.asInstanceOf[service.SetValue])
          |
          |      case "Get" =>
          |        entity.get(state, command.asInstanceOf[service.GetValue])
          |
          |      case _ =>
          |        throw new CommandHandlerNotFound(commandName)
          |    }
          |  }
          |  def handleEvent(state: MyState, event: Any): MyState = {
          |    event match {
          |      case evt: SetEvent =>
          |        entity.setEvent(state, evt)
          |
          |      case _ =>
          |        throw new EventHandlerNotFound(event.getClass)
          |    }
          |  }
          |}
          |""".stripMargin)
  }

  test("it can generate a provider") {
    val file =
      provider(testData.eventSourcedEntity(), testData.simpleEntityService())

    assertNoDiff(
      file.content,
      s"""|package com.example.service.domain
          |
          |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
          |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions
          |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider
          |import com.example.service
          |import com.external.ExternalDomainProto
          |import com.google.protobuf.Descriptors
          |import scala.collection.immutable.Seq
          |
          |// This code is managed by Akka Serverless tooling.
          |// It will be re-generated to reflect any changes to your protobuf definitions.
          |// DO NOT EDIT
          |
          |object MyEntityProvider {
          |  def apply(entityFactory: EventSourcedEntityContext => MyEntity): MyEntityProvider =
          |    new MyEntityProvider(entityFactory, EventSourcedEntityOptions.defaults)
          |}
          |class MyEntityProvider private(entityFactory: EventSourcedEntityContext => MyEntity, override val options: EventSourcedEntityOptions)
          |  extends EventSourcedEntityProvider[MyState, MyEntity] {
          |
          |  def withOptions(newOptions: EventSourcedEntityOptions): MyEntityProvider =
          |    new MyEntityProvider(entityFactory, newOptions)
          |
          |  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
          |    service.MyServiceProto.javaDescriptor.findServiceByName("MyService")
          |
          |  override final val entityType: String = "MyEntity"
          |
          |  override final def newRouter(context: EventSourcedEntityContext): MyEntityRouter =
          |    new MyEntityRouter(entityFactory(context))
          |
          |  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
          |    DomainProto.javaDescriptor ::
          |    service.MyServiceProto.javaDescriptor ::
          |    ExternalDomainProto.javaDescriptor :: Nil
          |}
          |""".stripMargin)
  }
}
