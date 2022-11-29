package com.example.actions;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Confirmed {

    public static Confirmed instance = new Confirmed();

    private Confirmed(){}
}