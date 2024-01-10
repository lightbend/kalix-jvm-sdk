/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.impl.replicatedentity

import io.grpc.Status.Code.INVALID_ARGUMENT
import kalix.javasdk.replicatedentity.CartEntity
import kalix.javasdk.replicatedentity.CartEntityProvider
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.testkit.TestProtocol
import kalix.testkit.replicatedentity.ReplicatedEntityMessages
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReplicatedEntitiesImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  import ReplicatedEntitiesImplSpec._
  import ShoppingCart.Item
  import ShoppingCart.Delta._
  import ShoppingCart.Protocol._
  import ReplicatedEntityMessages._

  private val service: TestReplicatedService = ShoppingCart.testService
  private val protocol: TestProtocol = TestProtocol(service.port)

  override def afterAll(): Unit = {
    protocol.terminate()
    service.terminate()
  }

  "ReplicatedEntitiesImpl" should {

    "manage entities with expected updates and deltas" in {
      protocol.replicatedEntity
        .connect()
        .send(init(ShoppingCart.Name, "cart"))
        .send(command(1, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(1, EmptyCart))
        .send(command(2, "cart", "AddItem", addItem("a", "apple", 1)))
        .expect(reply(2, EmptyJavaMessage, updated(domainLineItem("a", "apple", 1))))
        .send(command(3, "cart", "AddItem", addItem("a", "apple", 2)))
        .expect(reply(3, EmptyJavaMessage, updated(domainLineItem("a", "apple", 3))))
        .send(command(4, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(4, cart(Item("a", "apple", 3))))
        .send(command(5, "cart", "AddItem", addItem("b", "banana", 4)))
        .expect(reply(5, EmptyJavaMessage, updated(domainLineItem("b", "banana", 4))))
        .send(delta(update(domainLineItem("c", "cantaloupe", 5))))
        .send(delta(update(domainLineItem("b", "banana", 6))))
        .send(command(6, "cart", "AddItem", addItem("c", "cantaloupe", 7)))
        .expect(reply(6, EmptyJavaMessage, updated(domainLineItem("c", "cantaloupe", 12))))
        .send(command(7, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(7, cart(Item("a", "apple", 3), Item("b", "banana", 6), Item("c", "cantaloupe", 12))))
        .passivate()

      protocol.replicatedEntity
        .connect()
        .send( // reactivate with initial delta
          init(
            ShoppingCart.Name,
            "cart",
            update(
              domainLineItem("a", "apple", 3),
              domainLineItem("b", "banana", 6),
              domainLineItem("c", "cantaloupe", 12))))
        .send(command(1, "cart", "AddItem", addItem("a", "apple", 39)))
        .expect(reply(1, EmptyJavaMessage, updated(domainLineItem("a", "apple", 42))))
        .send(delta(remove("b", "c")))
        .send(command(2, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(2, cart(Item("a", "apple", 42))))
        .passivate()
    }

    "manage entities with expected delete commands" in {
      protocol.replicatedEntity
        .connect()
        .send(init(ShoppingCart.Name, "cart"))
        .send(command(1, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(1, EmptyCart))
        .send(command(2, "cart", "AddItem", addItem("a", "apple", 1)))
        .expect(reply(2, EmptyJavaMessage, updated(domainLineItem("a", "apple", 1))))
        .send(command(3, "cart", "RemoveCart", removeCart("cart")))
        .expect(reply(3, EmptyJavaMessage, deleted))
        .passivate()
    }

    "fail when first message is not init" in {
      service.expectLogError("Terminating entity due to unexpected failure") {
        val entity = protocol.replicatedEntity.connect()
        entity.send(command(1, "cart", "command"))
        val message = entity.expectNext()
        val failure = message.failure.get
        failure.description should startWith("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when entity is sent multiple init" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        val response = protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart"))
          .send(init(ShoppingCart.Name, "cart"))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail when service doesn't exist" in {
      service.expectLogError("Terminating entity [foo] due to unexpected failure") {
        protocol.replicatedEntity
          .connect()
          .send(init(serviceName = "DoesNotExist", entityId = "foo"))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail when command entity id is incorrect" in {
      service.expectLogError("Terminating entity [cart2] due to unexpected failure for command [foo]") {
        protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart1"))
          .send(command(1, "cart2", "foo"))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail when command payload is missing" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [foo]") {
        protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart"))
          .send(command(1, "cart", "foo", payload = None))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail when entity is sent empty message" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart"))
          .send(EmptyInMessage)
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail when delta doesn't match replicated data type" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart"))
          .send(delta(deltaCounter(42)))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail when command handler does not exist" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [foo]") {
        protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart"))
          .send(command(1, "cart", "foo"))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }

    "fail action when command handler returns error effect" in {
      protocol.replicatedEntity
        .connect()
        .send(init(ShoppingCart.Name, "cart"))
        .send(command(1, "cart", "AddItem", addItem("foo", "bar", -1)))
        .expect(failure(1, "Quantity for item foo must be greater than zero.", INVALID_ARGUMENT))
        .send(command(2, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(2, EmptyCart)) // check update-then-fail doesn't change entity state
        .passivate()

      protocol.replicatedEntity
        .connect()
        .send(init(ShoppingCart.Name, "cart"))
        .send(command(1, "cart", "GetCart", getShoppingCart("cart")))
        .expect(reply(1, EmptyCart))
        .passivate()
    }

    "fail when command handler throws exception" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [RemoveItem]") {
        protocol.replicatedEntity
          .connect()
          .send(init(ShoppingCart.Name, "cart"))
          .send(command(1, "cart", "RemoveItem", removeItem("foo")))
          .expectEntityFailure("Unexpected error")
          .expectClosed()
      }
    }
  }
}

object ReplicatedEntitiesImplSpec {
  object ShoppingCart {

    import com.example.replicatedentity.shoppingcart.ShoppingCartApi
    import com.example.replicatedentity.shoppingcart.domain.ShoppingCartDomain

    val Name: String = ShoppingCartApi.getDescriptor.findServiceByName("ShoppingCartService").getFullName

    def testService: TestReplicatedService =
      TestReplicatedEntity.service(
        CartEntityProvider
          .of(new CartEntity(_)))

    case class Item(id: String, name: String, quantity: Int)

    object Protocol {

      import scala.jdk.CollectionConverters._

      val EmptyCart: ShoppingCartApi.Cart = ShoppingCartApi.Cart.newBuilder.build

      def cart(items: Item*): ShoppingCartApi.Cart =
        ShoppingCartApi.Cart.newBuilder.addAllItems(lineItems(items)).build

      def lineItems(items: Seq[Item]): java.lang.Iterable[ShoppingCartApi.LineItem] =
        items.sortBy(_.id).map(item => lineItem(item.id, item.name, item.quantity)).asJava

      def lineItem(id: String, name: String, quantity: Int): ShoppingCartApi.LineItem =
        ShoppingCartApi.LineItem.newBuilder.setProductId(id).setName(name).setQuantity(quantity).build

      def getShoppingCart(id: String): ShoppingCartApi.GetShoppingCart =
        ShoppingCartApi.GetShoppingCart.newBuilder.setCartId(id).build

      def addItem(id: String, name: String, quantity: Int): ShoppingCartApi.AddLineItem =
        ShoppingCartApi.AddLineItem.newBuilder.setProductId(id).setName(name).setQuantity(quantity).build

      def removeItem(id: String): ShoppingCartApi.RemoveLineItem =
        ShoppingCartApi.RemoveLineItem.newBuilder.setProductId(id).build

      def removeCart(id: String): ShoppingCartApi.RemoveShoppingCart =
        ShoppingCartApi.RemoveShoppingCart.newBuilder.setCartId(id).build

      def domainLineItems(items: Seq[Item]): java.lang.Iterable[ShoppingCartDomain.LineItem] =
        items.sortBy(_.id).map(item => domainLineItem(item.id, item.name, item.quantity)).asJava

      def domainLineItem(id: String, name: String, quantity: Int): ShoppingCartDomain.LineItem =
        ShoppingCartDomain.LineItem.newBuilder.setProductId(id).setName(name).setQuantity(quantity).build
    }

    object Delta {
      import ReplicatedEntityMessages._

      def updated(items: ShoppingCartDomain.LineItem*): Effects =
        Effects(stateAction = replicatedEntityUpdate(update(items: _*)))

      def update(items: ShoppingCartDomain.LineItem*): ReplicatedEntityDelta.Delta =
        items
          .foldLeft(DeltaRegisterMap.empty) { case (delta, item) =>
            delta.update(primitiveString(item.getProductId), deltaRegister(item).value)
          }
          .replicatedEntityDelta()

      def removed(keys: String*): Effects =
        Effects(stateAction = replicatedEntityUpdate(remove(keys: _*)))

      def remove(keys: String*): ReplicatedEntityDelta.Delta =
        keys
          .foldLeft(DeltaRegisterMap.empty) { case (delta, key) =>
            delta.remove(primitiveString(key))
          }
          .replicatedEntityDelta()

      val deleted: Effects = Effects(stateAction = replicatedEntityDelete)
    }
  }
}
