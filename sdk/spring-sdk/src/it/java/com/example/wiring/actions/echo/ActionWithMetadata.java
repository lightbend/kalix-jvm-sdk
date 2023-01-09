package com.example.wiring.actions.echo;

import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.springsdk.KalixClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.ByteBuffer;
import java.util.List;

public class ActionWithMetadata extends Action{

  private KalixClient kalixClient;

  public ActionWithMetadata(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @GetMapping("/action-with-meta/{key}/{value}")
  public Effect<Message> actionWithMeta(@PathVariable String key, @PathVariable String value ){
    var def = kalixClient.get("/return-meta/"+key, Message.class);
    return effects().forward(def.withMetadata(List.of(stringValue(key, value))));
  }


  @GetMapping("/return-meta/{key}")
  public Effect<Message> returnMeta(@PathVariable String key){
    var metaValue = actionContext().metadata().get(key).get();
    return effects().reply(new Message(metaValue));
  }

  private Metadata.MetadataEntry stringValue(String key, String value) {
    return new Metadata.MetadataEntry() {
      @Override
      public String getKey() {
        return key;
      }

      @Override
      public String getValue() {
        return value;
      }

      @Override
      public ByteBuffer getBinaryValue() {
        return null;
      }

      @Override
      public boolean isText() {
        return true;
      }

      @Override
      public boolean isBinary() {
        return false;
      }
    };
  }
}
