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

import com.akkaserverless.codegen.scalasdk.impl.MainSourceGenerator
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.TestData

class MainSourceGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.scalaStyle

  def domainType(name: String): ModelBuilder.TypeArgument =
    ModelBuilder.TypeArgument(name, testData.domainProto())

  test("main source") {
    val entities = Map(
      "com.example.Entity1" -> testData.eventSourcedEntity(suffix = "1"),
      "com.example.Entity2" -> testData.valueEntity(suffix = "2"),
      "com.example.Entity3" -> testData.eventSourcedEntity(suffix = "3"),
      "com.example.Entity6" -> testData.replicatedEntity(
        ModelBuilder.ReplicatedSet(domainType("SomeElement")),
        suffix = "6"))

    val services = Map("com.example.Service1" -> testData.simpleActionService())

    val generatedSrc = MainSourceGenerator.mainSource(ModelBuilder.Model(services, entities)).content
    assertNoDiff(
      generatedSrc,
      """|package com.example
         |
         |import com.akkaserverless.scalasdk.AkkaServerless
         |import com.example.service.MyServiceAction
         |import com.example.service.domain.MyEntity1
         |import com.example.service.domain.MyEntity3
         |import com.example.service.domain.MyReplicatedEntity6
         |import com.example.service.domain.MyValueEntity2
         |import org.slf4j.LoggerFactory
         |
         |object Main {
         |
         |  private val log = LoggerFactory.getLogger("com.example.Main")
         |
         |  def createAkkaServerless(): AkkaServerless = {
         |    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
         |    // and is kept up-to-date with any changes in your protobuf definitions.
         |    // If you prefer, you may remove this and manually register these components in a
         |    // `AkkaServerless()` instance.
         |    AkkaServerlessFactory.withComponents(
         |      new MyEntity1(_),
         |      new MyEntity3(_),
         |      new MyReplicatedEntity6(_),
         |      new MyValueEntity2(_),
         |      new MyServiceAction(_))
         |  }
         |
         |  def main(args: Array[String]): Unit = {
         |    log.info("starting the Akka Serverless service")
         |    createAkkaServerless().start()
         |  }
         |}
         |""".stripMargin)
  }

  test("generated component registration source") {
    val service1Proto = testData.serviceProto("1")
    val service2Proto = testData.serviceProto("2")
    val service3Proto = testData.serviceProto("3").copy(pkg = "com.example.service.something")
    val service4Proto = testData.serviceProto("4").copy(pkg = "com.example.service.view")
    val service5Proto = testData.serviceProto("5")
    val service6Proto = testData.serviceProto("6")

    val services = Map(
      "com.example.Service1" -> testData.simpleEntityService(service1Proto, "1"),
      "com.example.Service2" -> testData.simpleEntityService(service2Proto, "2"),
      "com.example.Service3" -> testData.simpleEntityService(service3Proto, "3"),
      "com.example.Service4" -> testData.simpleViewService(service4Proto, "4"),
      "com.example.Service5" -> testData.simpleActionService(service5Proto),
      "com.example.Service6" -> testData.simpleEntityService(service6Proto, "6"))

    val entities = Map(
      "com.example.Entity1" -> testData.eventSourcedEntity(suffix = "1"),
      "com.example.Entity2" -> testData.valueEntity(suffix = "2"),
      "com.example.Entity3" -> testData.eventSourcedEntity(suffix = "3"),
      "com.example.Entity6" -> testData.replicatedEntity(
        ModelBuilder.ReplicatedSet(domainType("SomeElement")),
        suffix = "6"))

    val generatedSrc =
      MainSourceGenerator.akkaServerlessFactorySource(ModelBuilder.Model(services, entities)).content
    assertNoDiff(
      generatedSrc,
      """|package com.example
         |
         |import com.akkaserverless.scalasdk.AkkaServerless
         |import com.akkaserverless.scalasdk.action.ActionCreationContext
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
         |import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
         |import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
         |import com.akkaserverless.scalasdk.view.ViewCreationContext
         |import com.example.service.MyService5Action
         |import com.example.service.MyService5ActionProvider
         |import com.example.service.domain.MyEntity1
         |import com.example.service.domain.MyEntity1Provider
         |import com.example.service.domain.MyEntity3
         |import com.example.service.domain.MyEntity3Provider
         |import com.example.service.domain.MyReplicatedEntity6
         |import com.example.service.domain.MyReplicatedEntity6Provider
         |import com.example.service.domain.MyValueEntity2
         |import com.example.service.domain.MyValueEntity2Provider
         |import com.example.service.view.MyService4ViewImpl
         |import com.example.service.view.MyService4ViewProvider
         |
         |object AkkaServerlessFactory {
         |
         |  def withComponents(
         |      createMyEntity1: EventSourcedEntityContext => MyEntity1,
         |      createMyEntity3: EventSourcedEntityContext => MyEntity3,
         |      createMyReplicatedEntity6: ReplicatedEntityContext => MyReplicatedEntity6,
         |      createMyValueEntity2: ValueEntityContext => MyValueEntity2,
         |      createMyService4ViewImpl: ViewCreationContext => MyService4ViewImpl,
         |      createMyService5Action: ActionCreationContext => MyService5Action): AkkaServerless = {
         |    val akkaServerless = AkkaServerless()
         |    akkaServerless
         |      .register(MyEntity1Provider(createMyEntity1))
         |      .register(MyEntity3Provider(createMyEntity3))
         |      .register(MyReplicatedEntity6Provider(createMyReplicatedEntity6))
         |      .register(MyService4ViewProvider(createMyService4ViewImpl))
         |      .register(MyService5ActionProvider(createMyService5Action))
         |      .register(MyValueEntity2Provider(createMyValueEntity2))
         |  }
         |}
         |""".stripMargin)
  }

  test("generated component registration source for a view without update handlers") {
    val serviceProto = testData.serviceProto().copy(pkg = "com.example.service.view")

    val services = Map("com.example.Service" -> testData.simpleViewService(serviceProto).copy(transformedUpdates = Nil))

    val entities = Map.empty[String, ModelBuilder.Entity]

    val generatedSrc =
      MainSourceGenerator.akkaServerlessFactorySource(ModelBuilder.Model(services, entities)).content
    assertNoDiff(
      generatedSrc,
      """|package com.example
         |
         |import com.akkaserverless.scalasdk.AkkaServerless
         |import com.akkaserverless.scalasdk.view.ViewCreationContext
         |import com.example.service.view.MyServiceViewImpl
         |import com.example.service.view.MyServiceViewProvider
         |
         |object AkkaServerlessFactory {
         |
         |  def withComponents(
         |      createMyServiceViewImpl: ViewCreationContext => MyServiceViewImpl): AkkaServerless = {
         |    val akkaServerless = AkkaServerless()
         |    akkaServerless
         |      .register(MyServiceViewProvider(createMyServiceViewImpl))
         |  }
         |}
         |""".stripMargin)
  }

}
