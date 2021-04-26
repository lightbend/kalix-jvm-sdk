/*
 * Copyright 2019 Lightbend Inc.
 */

package shopping.cart;

import com.akkaserverless.javasdk.Jsonable;

import java.beans.BeanProperty;

/** The JSON formatted message to be read from a Pub/Sub topic. */
@Jsonable
public class TopicMessage {
  String operation;
  String userId;
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

  public String getUserId() {
    return userId;
  }

  @BeanProperty
  public void setUserId(String userId) {
    this.userId = userId;
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
