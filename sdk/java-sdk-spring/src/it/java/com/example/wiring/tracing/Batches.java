/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.tracing;

import java.util.List;

public record Batches(List<Batch> batches ){

    public record Batch(Resource resource, List<ScopeSpan> scopeSpans){
        public record Resource(List<Attribute> attributes){}

        public record ScopeSpan(Scope scope, List<Span> spans){
            public record Scope(String name){}
            public record Span(String traceId, String spanId, String name, String kind, List<Attribute> attributes){}
        }
    }

    public record Attribute(String key, Value value) {
        public record Value(String stringValue) {}
    }

}



