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

package com.lightbend.akkasls.codegen
package java

class ViewServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source") {

    val service = TestData.simpleViewService()

    val packageName = "com.example.service"
    val className = "MyServiceEntityImpl"
    val interfaceClassName = "AbstractMyServiceEntity"

    val sourceDoc =
      ViewServiceSourceGenerator.source(
        service,
        packageName,
        className,
        interfaceClassName
      )
    assertEquals(
      sourceDoc.layout,
      """/* This code was initialised by Akka Serverless tooling.
        | * As long as this file exists it will not be re-generated.
        | * You are free to make changes to this file.
        | */
        |
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.view.View;
        |import com.example.service.persistence.EntityOuterClass;
        |import java.util.Optional;
        |
        |/** A view. */
        |@View
        |public class MyServiceEntityImpl extends AbstractMyServiceEntity {
        |    @Override
        |    public ServiceOuterClass.ViewState created(EntityOuterClass.EntityCreated event, Optional<ServiceOuterClass.ViewState> state) {
        |        throw new RuntimeException("The update handler for `Created` is not implemented, yet");
        |    }
        |    
        |    @Override
        |    public ServiceOuterClass.ViewState updated(EntityOuterClass.EntityUpdated event, Optional<ServiceOuterClass.ViewState> state) {
        |        throw new RuntimeException("The update handler for `Updated` is not implemented, yet");
        |    }
        |}""".stripMargin
    )
  }

  test("interface source") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val sourceDoc =
      ViewServiceSourceGenerator.interfaceSource(service, packageName, service.fqn.name)
    assertEquals(
      sourceDoc.layout,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.view.*;
        |import com.example.service.persistence.EntityOuterClass;
        |import java.util.Optional;
        |
        |/** A view. */
        |public abstract class AbstractMyServiceView {
        |    @UpdateHandler
        |    public abstract ServiceOuterClass.ViewState created(EntityOuterClass.EntityCreated event, Optional<ServiceOuterClass.ViewState> state);
        |    
        |    @UpdateHandler
        |    public abstract ServiceOuterClass.ViewState updated(EntityOuterClass.EntityUpdated event, Optional<ServiceOuterClass.ViewState> state);
        |}""".stripMargin
    )
  }
}
