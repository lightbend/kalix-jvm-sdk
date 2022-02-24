package com.example;

import com.example.fibonacci.Number;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NumberSerdesTest {


  private ProtobufMapper protobufMapper = new ProtobufMapper();
  @Test
  public void serializeAndDeserialize() throws IOException {
    Number num = new Number(10);

    ProtobufSchema schemaWrapper = protobufMapper.generateSchemaFor(Number.class);

    byte[] bytes =
        protobufMapper
            .writerFor(Number.class)
            .with(schemaWrapper)
            .writeValueAsBytes(num);

    Number numDes =
        protobufMapper
            .readerFor(Number.class)
            .with(schemaWrapper)
            .readValue(bytes);

    assertEquals(10, numDes.value);

  }
}
