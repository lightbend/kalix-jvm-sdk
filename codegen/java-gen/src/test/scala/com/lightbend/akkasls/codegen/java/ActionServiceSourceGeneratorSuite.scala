/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

class ActionServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source") {

    val service = TestData.simpleActionService()

    val packageName        = "com.example.service"
    val className          = "MyServiceEntityImpl"
    val interfaceClassName = "MyServiceEntityInterface"

    val sourceDoc =
      ActionServiceSourceGenerator.source(
        service,
        packageName,
        className,
        interfaceClassName
      )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import akka.NotUsed;
      |import akka.stream.javadsl.Source;
      |import com.akkaserverless.javasdk.Reply;
      |import com.akkaserverless.javasdk.action.*;
      |import com.external.Empty;
      |
      |/** An action. */
      |@Action
      |public class MyServiceEntityImpl extends MyServiceEntityInterface {
      |    @Override
      |    public Reply<Empty> simpleMethod(ServiceOuterClass.MyRequest event, ActionContext ctx) {
      |        throw new RuntimeException("The command handler for `SimpleMethod` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public Source<Reply<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest event, ActionContext ctx) {
      |        throw new RuntimeException("The command handler for `StreamedOutputMethod` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public Source<Reply<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> event, ActionContext ctx) {
      |        throw new RuntimeException("The command handler for `FullStreamedMethod` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("interface source") {
    val service     = TestData.simpleActionService()
    val packageName = "com.example.service"
    val className   = "MyServiceEntity"

    val sourceDoc =
      ActionServiceSourceGenerator.interfaceSource(service, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import akka.NotUsed;
      |import akka.stream.javadsl.Source;
      |import com.akkaserverless.javasdk.Reply;
      |import com.akkaserverless.javasdk.action.*;
      |import com.external.Empty;
      |
      |/** An action. */
      |public abstract class MyServiceEntityInterface {
      |    @Handler
      |    public abstract Reply<Empty> simpleMethod(ServiceOuterClass.MyRequest event, ActionContext ctx);
      |    
      |    @Handler
      |    public abstract Source<Reply<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest event, ActionContext ctx);
      |    
      |    @Handler
      |    public abstract Source<Reply<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> event, ActionContext ctx);
      |}""".stripMargin
    )
  }
}
