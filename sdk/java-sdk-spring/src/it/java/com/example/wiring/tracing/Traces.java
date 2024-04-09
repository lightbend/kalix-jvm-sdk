/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.tracing;


import java.util.List;

public record Traces(List<Trace> traces) {
    public record Trace(String traceID, String rootServiceName, String rootTraceName) {
    }
}
