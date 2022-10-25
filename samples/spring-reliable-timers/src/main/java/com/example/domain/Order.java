package com.example.domain;


public record Order(String id,
                    boolean confirmed,
                    boolean placed,
                    String item,
                    int quantity) {

    public Order confirm() {
        return new Order(id, true, placed, item, quantity);
    }
}

