package com.example.api;

import com.example.domain.Customer;

import java.util.Collection;

// tag::record[]
public record CustomersResponse(Collection<Customer> customers) { }
// end::record[]
