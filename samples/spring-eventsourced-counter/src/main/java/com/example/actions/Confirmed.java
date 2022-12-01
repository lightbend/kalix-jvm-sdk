package com.example.actions;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public record Confirmed {

    public static Confirmed instance = new Confirmed();
}