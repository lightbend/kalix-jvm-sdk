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

package kalix.javasdk.impl.valueentity

import io.grpc.Status.Code.INVALID_ARGUMENT
import kalix.javasdk.valueentity.CartEntity
import kalix.javasdk.valueentity.CartEntityProvider
import kalix.testkit.TestProtocol
import kalix.testkit.valueentity.ValueEntityMessages
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValueEntitiesImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  import ValueEntitiesImplSpec._
  import ShoppingCart.Item
  import ShoppingCart.Protocol._
  import ValueEntityMessages._

  private val service: TestValueService = ShoppingCart.testService
  private val protocol: TestProtocol = TestProtocol(service.port)

  override def afterAll(): Unit = {
    protocol.terminate()
    service.terminate()
  }

  "EntityImpl" should {
    "fail when first message is not init" in {
      service.expectLogError("Terminating entity due to unexpected failure") {
        val entity = protocol.valueEntity.connect()
        entity.send(command(1, "cart", "command"))
        val message = entity.expectNext()
        val failure = message.failure.get
        failure.description should startWith("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when entity is sent multiple init" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when service doesn't exist" in {
      service.expectLogError("Terminating entity [foo] due to unexpected failure") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(serviceName = "DoesNotExist", entityId = "foo"))
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when command entity id is incorrect" in {
      service.expectLogError("Terminating entity [cart2] due to unexpected failure for command [foo]") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(ShoppingCart.Name, "cart1"))
        entity.send(command(1, "cart2", "foo"))
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when command payload is missing" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [foo]") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "foo", payload = None))
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when entity is sent empty message" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(EmptyInMessage)
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail when command handler does not exist" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [foo]") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "foo"))
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "fail action when command handler returns error effect" in {
      val entity = protocol.valueEntity.connect()
      entity.send(init(ShoppingCart.Name, "cart"))
      entity.send(command(1, "cart", "AddItem", addItem("foo", "bar", -1)))
      entity.expect(actionFailure(1, "Quantity for item foo must be greater than zero.", INVALID_ARGUMENT))
      entity.send(command(2, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(2, EmptyCart)) // check update-then-fail doesn't change entity state

      entity.passivate()
      val reactivated = protocol.valueEntity.connect()
      reactivated.send(init(ShoppingCart.Name, "cart"))
      reactivated.send(command(1, "cart", "GetCart", getShoppingCart("cart")))
      reactivated.expect(reply(1, EmptyCart))
      reactivated.passivate()
    }

    "fail when command handler throws exception" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [RemoveItem]") {
        val entity = protocol.valueEntity.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "RemoveItem", removeItem("foo")))
        entity.expectFailure("Unexpected error")
        entity.expectClosed()
      }
    }

    "manage entities with expected update commands" in {
      val entity = protocol.valueEntity.connect()
      entity.send(init(ShoppingCart.Name, "cart"))
      entity.send(command(1, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(1, EmptyCart))
      entity.send(command(2, "cart", "AddItem", addItem("abc", "apple", 1)))
      entity.expect(reply(2, EmptyJavaMessage, update(domainCart(Item("abc", "apple", 1)))))
      entity.send(command(3, "cart", "AddItem", addItem("abc", "apple", 2)))
      entity.expect(reply(3, EmptyJavaMessage, update(domainCart(Item("abc", "apple", 3)))))
      entity.send(command(4, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(4, cart(Item("abc", "apple", 3))))
      entity.send(command(5, "cart", "AddItem", addItem("123", "banana", 4)))
      entity.expect(reply(5, EmptyJavaMessage, update(domainCart(Item("abc", "apple", 3), Item("123", "banana", 4)))))

      entity.passivate()
      val reactivated = protocol.valueEntity.connect()
      reactivated.send(
        init(ShoppingCart.Name, "cart", state(domainCart(Item("abc", "apple", 3), Item("123", "banana", 4)))))
      reactivated.send(command(1, "cart", "AddItem", addItem("abc", "apple", 1)))
      reactivated.expect(
        reply(1, EmptyJavaMessage, update(domainCart(Item("abc", "apple", 4), Item("123", "banana", 4)))))
      reactivated.send(command(1, "cart", "GetCart", getShoppingCart("cart")))
      reactivated.expect(reply(1, cart(Item("abc", "apple", 4), Item("123", "banana", 4))))
      reactivated.passivate()
    }

    "manage entities with expected delete commands" in {
      val entity = protocol.valueEntity.connect()
      entity.send(init(ShoppingCart.Name, "cart"))
      entity.send(command(1, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(1, EmptyCart))
      entity.send(command(2, "cart", "AddItem", addItem("abc", "apple", 1)))
      entity.expect(reply(2, EmptyJavaMessage, update(domainCart(Item("abc", "apple", 1)))))
      entity.send(command(3, "cart", "AddItem", addItem("abc", "apple", 2)))
      entity.expect(reply(3, EmptyJavaMessage, update(domainCart(Item("abc", "apple", 3)))))
      entity.send(command(4, "cart", "RemoveCart", removeCart("cart")))
      entity.expect(reply(4, EmptyJavaMessage, delete()))
      entity.send(command(5, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(5, EmptyCart))
      entity.passivate()
    }
  }
}

object ValueEntitiesImplSpec {
  object ShoppingCart {

    import com.example.valueentity.shoppingcart.ShoppingCartApi
    import com.example.valueentity.shoppingcart.domain.ShoppingCartDomain

    val Name: String = ShoppingCartApi.getDescriptor.findServiceByName("ShoppingCartService").getFullName

    def testService: TestValueService =
      TestValueEntity.service(
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

      def domainCart(items: Item*): ShoppingCartDomain.Cart =
        ShoppingCartDomain.Cart.newBuilder.addAllItems(domainLineItems(items)).build
    }
  }
}
