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

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors
import com.google.protobuf.ExtensionRegistry

import com.lightbend.akkasls.codegen.{FullyQualifiedName, Log, ModelBuilder, PackageNaming}

import java.io.FileInputStream
import java.nio.file.Paths
import scala.util.Using
import scala.jdk.CollectionConverters._
import scala.collection.mutable
import org.slf4j.LoggerFactory

class EventSourcedEntityTestKitGeneratorSuite extends munit.FunSuite {

  test("it can generate the method") {

    val packageName = "com.example.shoppingcart"
    val entity = generateShoppingCartEntity
    val service = generateShoppingCartService(entity)
    val className = "ShoppingCart"

    val sourceCode = EventSourcedEntityTestKitGenerator.generateSourceCode(
      service,
      entity,
      packageName,
      className
    )

    val expected =
      """
            |package com.example.shoppingcart.domain;
            |
            |import com.example.shoppingcart.domain.ShoppingCart;
            |import com.example.shoppingcart.domain.ShoppingCartDomain;
            |import com.example.shoppingcart.ShoppingCartApi;
            |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
            |import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
            |import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
            |import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
            |import com.akkaserverless.javasdk.testkit.AkkaserverlessTestKit;
            |import com.akkaserverless.javasdk.testkit.Result;
            |import com.google.protobuf.Empty;
            |import java.util.ArrayList;
            |import java.util.List;
            |import java.util.NoSuchElementException;
            |import scala.jdk.javaapi.CollectionConverters;
            |
            |public class ShoppingCartTestKit {
            |    
            |    private ShoppingCartDomain.Cart state;
            |    private ShoppingCart entity;
            |    private List<Object> events = new ArrayList<Object>();
            |    private AkkaserverlessTestKit helper = new AkkaserverlessTestKit<ShoppingCartDomain.Cart>();
            |
            |    public ShoppingCartTestKit(String entityId){
            |        this.state = ShoppingCartDomain.Cart.newBuilder().build();
            |        this.entity = new ShoppingCart(entityId);
            |    }
            |
            |    public ShoppingCartTestKit(String entityId, ShoppingCartDomain.Cart state){
            |        this.state = state;
            |        this.entity = new ShoppingCart(entityId);
            |    }
            |
            |    public ShoppingCartDomain.Cart getState(){
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

    assertEquals(sourceCode.layout, expected)
  }

  /**
   * This is equivalent to the .proto in
   *
  **/
  def generateShoppingCartEntity(): ModelBuilder.EventSourcedEntity = {

    val domainProto =
      PackageNaming(
        "ShoppingCartDomain",
        "com.example.shoppingcart.domain",
        None,
        None,
        Some("ShoppingCartDomain"),
        javaMultipleFiles = false
      )

    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName("ShoppingCart", domainProto),
      "eventsourced-shopping-cart",
      Some(ModelBuilder.State(FullyQualifiedName("Cart", domainProto))),
      List(
        ModelBuilder.Event(FullyQualifiedName("ItemAdded", domainProto)),
        ModelBuilder.Event(FullyQualifiedName("ItemRemoved", domainProto))
      )
    )
  }

  def generateShoppingCartService(entity: ModelBuilder.Entity): ModelBuilder.EntityService = {
    val shoppingCartProto =
      PackageNaming(
        "ShoppingCartApi",
        "com.example.shoppingcart",
        None,
        None,
        Some("ShoppingCartApi"),
        javaMultipleFiles = false
      )
    val googleEmptyProto =
      PackageNaming(
        "Empty",
        "google.protobuf",
        Some("google.golang.org/protobuf/types/known/emptypb"),
        Some("com.google.protobuf"),
        Some("EmptyProto"),
        javaMultipleFiles = true
      )
    ModelBuilder.EntityService(
      FullyQualifiedName("ShoppingCartService", shoppingCartProto),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("AddItem", shoppingCartProto),
          FullyQualifiedName("AddLineItem", shoppingCartProto),
          FullyQualifiedName("Empty", googleEmptyProto),
          streamedInput = false,
          streamedOutput = false
        ),
        ModelBuilder.Command(
          FullyQualifiedName("RemoveItem", shoppingCartProto),
          FullyQualifiedName("RemoveLineItem", shoppingCartProto),
          FullyQualifiedName("Empty", googleEmptyProto),
          streamedInput = false,
          streamedOutput = false
        ),
        ModelBuilder.Command(
          FullyQualifiedName("GetCart", shoppingCartProto),
          FullyQualifiedName("GetShoppingCart", shoppingCartProto),
          FullyQualifiedName("Cart", shoppingCartProto),
          streamedInput = false,
          streamedOutput = false
        )
      ),
      entity.fqn.fullName
    )
  }

}
