package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.testkit.Result;
import com.akkaserverless.javasdk.testkit.impl.AkkaServerlessTestKitHelper;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import java.util.List;

public class ShoppingCartTestKit {


  /**
   * Create a testkit instance of ShoppingCart
   * @param entityFactory A function that creates a ShoppingCart based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   */
  public static ShoppingCartTestKit of(Function<EventSourcedEntityContext, ShoppingCart> entityFactory) // <1>

  /**
   * Create a testkit instance of ShoppingCart with a specific entity id.
   */
  public static ShoppingCartTestKit of(String entityId, Function<EventSourcedEntityContext, ShoppingCart> entityFactory) // <2>

  /**
   * @return The current state of the ShoppingCart under test
   */
  public ShoppingCartDomain.Cart getState() // <3>

  /**
   * @return All events that has been emitted by command handlers since the creation of this testkit.
   *         Individual sets of events from a single command handler invokation can be found in the
   *         Result from calling it.
   */
  public List<Object> getAllEvents() // <4>

  public EventSourcedResult<Empty> addItem(ShoppingCartApi.AddLineItem command) // <5>

  public EventSourcedResult<Empty> removeItem(ShoppingCartApi.RemoveLineItem command) // <5>

  public EventSourcedResult<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart command) // <5>
  
}