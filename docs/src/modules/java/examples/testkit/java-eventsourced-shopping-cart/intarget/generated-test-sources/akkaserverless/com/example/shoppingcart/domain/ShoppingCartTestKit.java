package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.testkit.Result;
import com.akkaserverless.javasdk.testkit.impl.AkkaServerlessTestKitHelper;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Empty;
import java.util.List;

public class ShoppingCartTestKit {


  public ShoppingCartTestKit(ShoppingCart entity) // <1>

  public ShoppingCartTestKit(ShoppingCart entity, ShoppingCartDomain.Cart state) // <2>

  public ShoppingCartDomain.Cart getState() // <3>

  public List<Object> getAllEvents() // <4>

  public Result<Empty> addItem(ShoppingCartApi.AddLineItem command) // <5>
  
}