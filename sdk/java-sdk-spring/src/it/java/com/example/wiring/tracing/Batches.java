/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.tracing;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // Method to find a Batch with a Scope with a specific name
    // This is the name of the tracer
    public static List<Batch> findBatchesWithScopeName(Batches batches, String scopeName) {
        return batches.batches().stream()
                .filter(batch ->
                        batch.scopeSpans().stream().anyMatch(scopeSpan -> scopeSpan.scope().name().equals(scopeName)
                )).toList();
    }

    public static List<Batch.ScopeSpan.Span> findSpansWithAttribute(List<Batch> batches, String key, String value) {
       return batches.stream().flatMap(batch -> findSpansWithAttribute(batch, key, value).stream()).collect(Collectors.toList());
    }


    public static List<Batch.ScopeSpan.Span> findSpansWithName(List<Batch> batches , String name) {
        return batches.stream().flatMap(batch -> findSpansWithName(batch, name).stream()).collect(Collectors.toList());
    }

    public static List<Batch.ScopeSpan.Span> findSpansWithAttribute(Batch batch, String key, String value) {
       return batch.scopeSpans().stream().flatMap( scopeSpan ->
                scopeSpan.spans.stream()
                        .filter( span -> span.attributes().stream()
                                .anyMatch( p -> p.key.equals(key) && p.value.stringValue.equals(value)))).toList();
    }

    public static List<Batch.ScopeSpan.Span> findSpansWithName(Batch batch, String name) {
        return batch.scopeSpans().stream().flatMap( scopeSpan ->
                scopeSpan.spans.stream()
                        .filter( span -> span.name.equals(name))).toList();

    }






}



