package customer.api;

import customer.domain.Customer;

import java.util.Collection;

// tag::record[]
public record CustomersResponse(Collection<Customer> customers) { }
// end::record[]
