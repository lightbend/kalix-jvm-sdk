package store.order.api;

public record CreateOrder(String productId, String customerId, int quantity) {}
