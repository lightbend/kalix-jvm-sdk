package com.example.shoppingcart.domain

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import com.akkaserverless.javasdk.impl.effect.{SecondaryEffectImpl, MessageReplyImpl}
import com.google.protobuf.Empty

import com.example.shoppingcart.ShoppingCartApi


// case class Result[EffectType, Event](reply: SecondaryEffectImpl, events: List[Event])
case class Result[EffectType, Event](reply: Empty, events: List[Event])

//TODO add types
class EventSourcedTestKit {

	def runCommand(serviceName: String, command: ShoppingCartApi.AddLineItem, state: ShoppingCartDomain.Cart): Result[Empty, ShoppingCartDomain.ItemAdded] = {

		//TODO fake it later on
		val expectedEvent: ShoppingCartDomain.ItemAdded = ShoppingCartDomain.ItemAdded.newBuilder().setItem(
                    ShoppingCartDomain.LineItem.newBuilder()
                            .setProductId("id1")
                            .setName("name")
                            .setQuantity(2)
                            .build()).build()
		// val eseei = new EventSourcedEntityEffectImpl[State]()//State => ShoppingCartDomain.Cart
		Result[Empty, ShoppingCartDomain.ItemAdded](Empty.getDefaultInstance(),List(expectedEvent))
	}	
}


// case class CommandResultImpl[Command, Event, State, Reply](
//       command: Command,
//       events: immutable.Seq[Event],
//       state: State,
//       replyOption: Option[Reply])




































// class EventSourcedTestKit {

// 	var entity = 

// 	//I should be creating an instance with access to a behavior or equivalent
// 		//how can create the equivalent so I have access to the Commands, Events and State/entity?
// 	def create[ShoppingCartApi,ShoppingCartDomain](entity: Entity): = 
// }


// // the question here is how is it possible that I can use multiple commands 
// // in this class if I set only one on construction. 
// 		// If I use the ShoppingCartApi would that help? how could I use it?
// // it seems they use CommandHandler or CommandHandlerWithReply such it has a builder with all possible commands.  
// class EventSourcedTestKitImpl {

// 	def apply(command: )

// }

object FakeReflection {

	// def run(command: Command, serviceName: String): Result = {
	// 	serviceName.toLowerCase match {
	// 		case "additem" => 
	// 	}
	// }
}




// 	Effect reply()

// 	List[Event] events()

// 	Throwable errors()

// 	// Forwardable forward()
// }

///---

// 

