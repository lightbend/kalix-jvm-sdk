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

import com.google.common.base.Charsets
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes
import com.lightbend.akkasls.codegen.java.SourceGenerator._
import _root_.java.nio.file.{Files, Path}

object EventSourcedEntityTestKitGenerator {

  def generate(entity: ModelBuilder.Entity,
               service: ModelBuilder.EntityService,
               generatedSourceDirectory: Path): Iterable[Path] = {
    val packageName = entity.fqn.parent.javaPackage
    val className = entity.fqn.name
    val sourceCode = generateSource(service, entity, packageName, className)

    val packagePath = packageAsPath(packageName)
    val generatedSourceDirectoryPath = generatedSourceDirectory.resolve(packagePath.resolve(className + "TestKit.java"))

    if (!generatedSourceDirectoryPath.toFile.exists) {
      Files.write(generatedSourceDirectoryPath, sourceCode.layout.getBytes(Charsets.UTF_8))
      List(generatedSourceDirectoryPath)
    } else {
      Nil
    }

  }

  private[codegen] def generateSource(service: ModelBuilder.EntityService,
                                      entity: ModelBuilder.Entity,
                                      packageName: String,
                                      className: String): PrettyPrinterTypes.Document = {
    entity match {
      case entity: ModelBuilder.EventSourcedEntity => generateSourceCode(service, entity, packageName, className)
      case entity: ModelBuilder.ValueEntity => PrettyPrinterTypes.emptyDocument
    }
  }

  private[codegen] def generateSourceCode(service: ModelBuilder.EntityService,
                                          entity: ModelBuilder.EventSourcedEntity,
                                          packageName: String,
                                          className: String): PrettyPrinterTypes.Document = {
    //Review I can add them with this
    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "scala.jdk.javaapi.CollectionConverters",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase",
        "com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl",
        "com.akkaserverless.javasdk.impl.effect.MessageReplyImpl",
        "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl",
        "com.akkaserverless.javasdk.testkit.AkkaserverlessTestKit",
        "com.akkaserverless.javasdk.testkit.Result"
      ) //TODO find out why this is added when generate Imports
    ).replace(s"import ${entity.fqn.parent.pkg}.${entity.fqn.parent.name};\n", "")

    val domainClassName = entity.fqn.parent.name
    val entityClassName = entity.fqn.name
    val entityStateName = entity.state.get.fqn.name //TODO Q Why [Option], is it possible not to have `state`?

    val testkitClassName = s"${entityClassName}TestKit"

    pretty(
      s"""
            |package ${entity.fqn.parent.pkg};
            |
            |import ${entity.fqn.parent.pkg}.$entityClassName;
            |import ${entity.fqn.parent.pkg}.$domainClassName;
            |import ${service.fqn.parent.pkg}.${service.fqn.parent.name};
            |$imports
            |
            |public class ${testkitClassName} {
            |
            |    private ${domainClassName}.${entityStateName} state;
            |    private ${entityClassName} entity;
            |    private List<Object> events = new ArrayList<Object>();
            |    private AkkaserverlessTestKit helper = new AkkaserverlessTestKit<${domainClassName}.${entityStateName}>();
            |
            |    public ${testkitClassName}(${entityClassName} entity){
            |        this.state = entity.emptyState();
            |        this.entity = entity;
            |    }
            |
            |    public ${testkitClassName}(${entityClassName} entity, ${domainClassName}.${entityStateName} state){
            |        this.state = state;
            |        this.entity = entity;
            |    }
            |
            |    public ${domainClassName}.${entityStateName} getState(){
            |            return state;
            |    }
            |
            |    public List<Object> getAllEvents(){
            |        return this.events;
            |    }
            |
            |    private <Reply> List<Object> getEvents(EventSourcedEntityBase.Effect<Reply> effect){
            |        return CollectionConverters.asJava(helper.getEvents(effect));
            |    }
            |
            |    private <Reply> Reply getReplyOfType(EventSourcedEntityBase.Effect<Reply> effect, ShoppingCartDomain.Cart state){
            |        return (Reply) helper.getReply(effect, state);
            |    }
            |
            |    private ${domainClassName}.${entityStateName} handleEvent(${domainClassName}.${entityStateName} state, Object event) {
            |        ${Syntax.indent(generateHandleEvents(entity.events, domainClassName), 8)}
            |    }
            |
            |    private <Reply> Result<Reply> handleCommand(EventSourcedEntityBase.Effect<Reply> effect){
            |        List<Object> events = getEvents(effect); 
            |        this.events.add(events);
            |        for(Object e: events){
            |            this.state = handleEvent(state,e);
            |        }
            |        Reply reply = this.<Reply>getReplyOfType(effect, this.state);
            |        return new Result(reply, CollectionConverters.asScala(events));
            |    }
            |    ${Syntax.indent(generateServices(service.commands), 4)}
            |}""".stripMargin
    )

  }

  def generateServices(commands: Iterable[ModelBuilder.Command]): String = {
    require(!commands.isEmpty, "empty `commands` not allowed")

    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        command.fqn.parent.name + "." + command.outputType.name
      }

    commands
      .map { command =>
        s"""
        |public Result<${selectOutput(command)}> ${lowerFirst(command.fqn.name)}(ShoppingCartApi.${command.inputType.name} command) {
        |    EventSourcedEntityBase.Effect<${selectOutput(command)}> effect = entity.${lowerFirst(command.fqn.name)}(state, command);
        |    return handleCommand(effect);
        |}""".stripMargin
      }
      .mkString("\n")
  }

  //TODO This method should be deleted when the codegen CartHandler.handleEvents gets available
  def generateHandleEvents(events: Iterable[ModelBuilder.Event], domainClassName: String): String = {
    require(events.nonEmpty, "empty `events` not allowed")

    val top =
      s"""|if (event instanceof ${domainClassName}.${events.head.fqn.name}) {
          |    return entity.${lowerFirst(events.head.fqn.name)}(state, (${domainClassName}.${events.head.fqn.name}) event);""".stripMargin

    val middle = events.tail.map { event =>
      s"""
        |} else if (event instanceof ${domainClassName}.${event.fqn.name}) {
        |    return entity.${lowerFirst(event.fqn.name)}(state, (${domainClassName}.${event.fqn.name}) event);""".stripMargin
    }

    val bottom =
      s"""
        |} else {
        |    throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
        |}""".stripMargin

    top + middle.mkString("") + bottom
  }

}
