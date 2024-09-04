package com.example.json;

import kalix.javasdk.JsonSupport;
import kalix.javasdk.testkit.ActionResult;
import com.example.json.MyServiceAction;
import com.example.json.MyServiceActionTestKit;
import com.example.json.MyServiceApi;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyServiceActionTest {

  @Test
  public void consumeTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    ActionResult<Empty> result = testKit.consume(JsonSupport.encodeJson(new JsonKeyValueMessage("key", 5)));
    assertEquals(Empty.getDefaultInstance(), result.getReply());
  }

  @Test
  public void produceTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    ActionResult<Any> result = testKit.produce(MyServiceApi.KeyValue.newBuilder().setKey("key").setValue(5).build());
    JsonKeyValueMessage decoded = JsonSupport.decodeJson(JsonKeyValueMessage.class, result.getReply());
    assertEquals("key", decoded.key);
    assertEquals(5, decoded.value);
  }

}
