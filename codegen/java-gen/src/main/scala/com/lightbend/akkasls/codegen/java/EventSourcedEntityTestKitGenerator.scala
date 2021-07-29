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

package com.lightbend.akkasls.codegen.testkit

import com.lightbend.akkasls.codegen.ModelBuilder
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document
import com.lightbend.akkasls.codegen.java.SourceGenerator._

object EventSourcedEntityTestKitGenerator {

  def generateSourceCode(service: ModelBuilder.EntityService,
                         entity: ModelBuilder.EventSourcedEntity,
                         packageName: String,
                         className: String): Document = {

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
            |    public ${testkitClassName}(String entityId){
            |        this.state = ${domainClassName}.${entityStateName}.newBuilder().build();
            |        this.entity = new ${entityClassName}(entityId);
            |    }
            |
            |    public ${testkitClassName}(String entityId, ${domainClassName}.${entityStateName} state){
            |        this.state = state;
            |        this.entity = new ${entityClassName}(entityId);
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
            |    private List<Object> getEvents(EventSourcedEntityBase.Effect<Empty> effect){
            |        return CollectionConverters.asJava(helper.getEvents(effect));
            |    }
            |
            |    // WIP - dealing with different replies. Forward, Error maybe even no reply
            |    private <Reply> Reply getReplyOfType(EventSourcedEntityBase.Effect<Empty> effect, ShoppingCartDomain.Cart state, Class<Reply> expectedClass){
            |        return (Reply) helper.getReply(effect, state);
            |    }
            |}""".stripMargin
    )

  }

}
