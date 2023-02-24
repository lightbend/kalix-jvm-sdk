package com.example.domain;

public record OrderStatus(String orderId,
                          String item,
                          int quantity,
                          boolean confirmed) {
}

