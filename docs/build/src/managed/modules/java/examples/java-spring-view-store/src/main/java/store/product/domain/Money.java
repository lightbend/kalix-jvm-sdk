package store.product.domain;

// tag::domain[]
public record Money(String currency, long units, int cents) {}
// end::domain[]
