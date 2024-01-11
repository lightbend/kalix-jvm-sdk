/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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



