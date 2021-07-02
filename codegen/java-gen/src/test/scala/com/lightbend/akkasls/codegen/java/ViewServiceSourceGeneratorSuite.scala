/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

class ViewServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source") {

    val service = TestData.simpleViewService()

    val packageName        = "com.example.service"
    val className          = "MyServiceEntityImpl"
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
        |import com.akkaserverless.javasdk.view.*;
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
    val service     = TestData.simpleViewService()
    val packageName = "com.example.service"
    val className   = "MyServiceEntity"

    val sourceDoc =
      ViewServiceSourceGenerator.interfaceSource(service, packageName, className)
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
        |public abstract class AbstractMyServiceEntity {
        |    @UpdateHandler
        |    public abstract ServiceOuterClass.ViewState created(EntityOuterClass.EntityCreated event, Optional<ServiceOuterClass.ViewState> state);
        |    
        |    @UpdateHandler
        |    public abstract ServiceOuterClass.ViewState updated(EntityOuterClass.EntityUpdated event, Optional<ServiceOuterClass.ViewState> state);
        |}""".stripMargin
    )
  }
}
