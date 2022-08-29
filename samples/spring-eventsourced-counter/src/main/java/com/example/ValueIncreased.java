package com.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueIncreased {

    public final long value;

    @JsonCreator
    public ValueIncreased(@JsonProperty("value") long value) {
        this.value = value;
    }
}
