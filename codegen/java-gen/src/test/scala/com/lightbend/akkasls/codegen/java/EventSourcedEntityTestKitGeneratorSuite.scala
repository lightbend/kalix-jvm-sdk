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

class EventSourcedEntityTestKitGeneratorSuite extends munit.FunSuite {

  test(
    "it can generate an specific TestKit for the proto files " +
    "in test/resources/testkit"
  ) {

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
      """/* This code is managed by Akka Serverless tooling.
      | * It will be re-generated to reflect any changes to your protobuf definitions.
      | * DO NOT EDIT
      | */
      |package com.example.shoppingcart.domain;
      |
      |import com.example.shoppingcart.domain.ShoppingCart;
      |import com.example.shoppingcart.domain.ShoppingCartDomain;
      |import com.example.shoppingcart.ShoppingCartApi;
      |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
      |import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
      |import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
      |import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
      |import com.akkaserverless.javasdk.testkit.Result;
      |import com.akkaserverless.javasdk.testkit.internal.AkkaServerlessTestKitHelper;
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
      |    private AkkaServerlessTestKitHelper helper = new AkkaServerlessTestKitHelper<ShoppingCartDomain.Cart>();
      |
      |    public ShoppingCartTestKit(ShoppingCart entity){
      |        this.state = entity.emptyState();
      |        this.entity = entity;
      |    }
      |
      |    public ShoppingCartTestKit(ShoppingCart entity, ShoppingCartDomain.Cart state){
      |        this.state = state;
      |        this.entity = entity;
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
      |    private <Reply> List<Object> getEvents(EventSourcedEntity.Effect<Reply> effect){
      |        return CollectionConverters.asJava(helper.getEvents(effect));
      |    }
      |
      |    private <Reply> Reply getReplyOfType(EventSourcedEntity.Effect<Reply> effect, ShoppingCartDomain.Cart state){
      |        return (Reply) helper.getReply(effect, state);
      |    }
      |
      |    private ShoppingCartDomain.Cart handleEvent(ShoppingCartDomain.Cart state, Object event) {
      |        if (event instanceof ShoppingCartDomain.ItemAdded) {
      |            return entity.itemAdded(state, (ShoppingCartDomain.ItemAdded) event);
      |        } else if (event instanceof ShoppingCartDomain.ItemRemoved) {
      |            return entity.itemRemoved(state, (ShoppingCartDomain.ItemRemoved) event);
      |        } else {
      |            throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
      |        }
      |    }
      |
      |    private <Reply> Result<Reply> interpretEffects(EventSourcedEntity.Effect<Reply> effect){
      |        List<Object> events = getEvents(effect); 
      |        this.events.add(events);
      |        for(Object e: events){
      |            this.state = handleEvent(state,e);
      |        }
      |        Reply reply = this.<Reply>getReplyOfType(effect, this.state);
      |        return new Result(reply, events);
      |    }
      |
      |    public Result<Empty> addItem(ShoppingCartApi.AddLineItem command) {
      |        EventSourcedEntity.Effect<Empty> effect = entity.addItem(state, command);
      |        return interpretEffects(effect);
      |    }
      |
      |    public Result<Empty> removeItem(ShoppingCartApi.RemoveLineItem command) {
      |        EventSourcedEntity.Effect<Empty> effect = entity.removeItem(state, command);
      |        return interpretEffects(effect);
      |    }
      |
      |    public Result<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart command) {
      |        EventSourcedEntity.Effect<ShoppingCartApi.Cart> effect = entity.getCart(state, command);
      |        return interpretEffects(effect);
      |    }
      |}""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

  /**
   * This ModelBuilder.EventSourcedEntity is equivalent to the
   * entity in test/resources/testkit/shoppingcart_domain.proto
  **/
  def generateShoppingCartEntity(): ModelBuilder.EventSourcedEntity = {

    val domainProto =
      PackageNaming(
        "ShoppingcartDomain", // Cart here is lowerCase as per protobuf generation
        "com.example.shoppingcart.domain",
        None,
        None,
        Some("ShoppingCartDomain"),
        javaMultipleFiles = false
      )

    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName("ShoppingCart", domainProto),
      "eventsourced-shopping-cart",
      ModelBuilder.State(FullyQualifiedName("Cart", domainProto)),
      List(
        ModelBuilder.Event(FullyQualifiedName("ItemAdded", domainProto)),
        ModelBuilder.Event(FullyQualifiedName("ItemRemoved", domainProto))
      )
    )
  }

  /**
   * This ModelBuilder.EntityService is equivalent to
   * service in test/resources/testkit/shoppingcart_api.proto
  **/
  def generateShoppingCartService(entity: ModelBuilder.Entity): ModelBuilder.EntityService = {
    val shoppingCartProto =
      PackageNaming(
        "ShoppingcartApi", // Cart here is lowerCase as per protobuf generation
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
