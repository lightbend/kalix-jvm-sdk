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

class ActionServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source") {

    val service = TestData.simpleActionService()

    val packageName = "com.example.service"
    val className = "MyServiceEntityImpl"
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
      """/* This code was initialised by Akka Serverless tooling.
        | * As long as this file exists it will not be re-generated.
        | * You are free to make changes to this file.
        | */
        |
        |package com.example.service;
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
    val service = TestData.simpleActionService()
    val packageName = "com.example.service"
    val className = "MyServiceEntity"

    val sourceDoc =
      ActionServiceSourceGenerator.interfaceSource(service, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
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
