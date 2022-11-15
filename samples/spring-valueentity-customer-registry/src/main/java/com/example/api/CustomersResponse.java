package com.example.api;

import com.example.domain.Customer;

import java.util.Collection;

public record CustomersResponse(Collection<Customer> results) { }
