package store.view.nested;

import store.product.domain.Money;

// tag::nested[]
public record CustomerOrder(
    String customerId,
    String orderId,
    String productId,
    String productName,
    Money price,
    int quantity,
    long createdTimestamp) {}
// end::nested[]
