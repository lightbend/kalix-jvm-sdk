/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.javasdk.client;

import com.example.wiring.tracing.Batches;
import com.example.wiring.tracing.Traces;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

public class TraceDecoderTest {

    @Test
    public void findTwoActions() throws JsonProcessingException, MalformedURLException, IOException {
        ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        File file = new File("src/test/resources/traces_actions.json");
        Traces t = om.readValue(file, Traces.class);
        assert(t.traces().size() == 2);
        assert(t.traces().get(0).traceID().equals("4e2de484e6d2a80b6672a98481c59671"));
    }

    @Test
    public void findProxyESEandAction() throws IOException {
        ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        File file = new File("src/test/resources/traces_batches.json");
        Batches bs = om.readValue(file, Batches.class );
        assert(bs.batches().size() == 3);
        assert(bs.batches().get(0).scopeSpans().size() == 1);
        assert(bs.batches().get(0).scopeSpans().get(0).spans().get(0).spanId().equals("ui2l+0OuGn8="));
        assert(bs.batches().get(0).scopeSpans().get(0).scope().name().equals("kalix.proxy.telemetry.TraceInstrumentationImpl"));

    }
}





