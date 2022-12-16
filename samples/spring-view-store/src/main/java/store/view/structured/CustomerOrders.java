package store.view.structured;

import java.util.List;

// tag::structured[]
public record CustomerOrders(
    String id,
    CustomerShipping shipping,
    List<ProductOrder> orders) {}
// end::structured[]
