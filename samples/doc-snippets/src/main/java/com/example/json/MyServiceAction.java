/* This code was generated by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.json;

// tag::action[]
import com.akkaserverless.javasdk.JsonSupport;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyServiceAction extends AbstractMyServiceAction {

  private static final Logger LOG = LoggerFactory.getLogger(MyServiceAction.class);

  public MyServiceAction(ActionCreationContext creationContext) {}

  @Override
  public Effect<Empty> consume(Any any) {
    JsonKeyValueMessage jsonMessage =
        JsonSupport.decodeJson(JsonKeyValueMessage.class, any); // <1>
    LOG.info("Consumed " + jsonMessage);
    return effects().reply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Any> produce(MyServiceApi.KeyValue keyValue) {
    JsonKeyValueMessage jsonMessage =
        new JsonKeyValueMessage(keyValue.getKey(), keyValue.getValue()); // <2>
    Any jsonAny = JsonSupport.encodeJson(jsonMessage); // <3>
    return effects().reply(jsonAny);
  }
}
// end::action[]
