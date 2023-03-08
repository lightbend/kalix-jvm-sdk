package store.view.structured;

// tag::structured[]
public record ProductOrder(
    String id,
    String name,
    int quantity,
    ProductValue value,
    String orderId,
    long orderCreatedTimestamp) {}
// end::structured[]
