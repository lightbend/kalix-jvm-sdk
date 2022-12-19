package store.order.domain;

// tag::domain[]
public record Order(
    String orderId,
    String productId,
    String customerId,
    int quantity,
    long createdTimestamp) {}
// end::domain[]
