/*
 * Copyright 2021 Lightbend Inc.
 */

package shopping.cart;

import com.akkaserverless.javasdk.Jsonable;

import java.beans.BeanProperty;

/** The JSON formatted message to be read from a Pub/Sub topic. */
@Jsonable
public class TopicMessage {
  String operation;
  String cartId;
  String productId;
  String name;
  int quantity;

  public String getOperation() {
    return operation;
  }

  @BeanProperty
  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getCartId() {
    return cartId;
  }

  @BeanProperty
  public void setCartId(String cartId) {
    this.cartId = cartId;
  }

  public String getProductId() {
    return productId;
  }

  @BeanProperty
  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getName() {
    return name;
  }

  @BeanProperty
  public void setName(String name) {
    this.name = name;
  }

  public int getQuantity() {
    return quantity;
  }

  @BeanProperty
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
