package com.example.wiring.valueentities.headers;

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.ForwardHeaders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.example.wiring.actions.headers.ForwardHeadersAction.SOME_HEADER;

@EntityKey("id")
@EntityType("forward-headers-ve")
@RequestMapping("/forward-headers-ve/{id}")
@ForwardHeaders(SOME_HEADER)
public class ForwardHeadersValueEntity extends ValueEntity<String> {

  @PutMapping()
  public Effect<Message> createUser() {
    String headerValue = commandContext().metadata().get(SOME_HEADER).orElse("");
    return effects().reply(new Message(headerValue));
  }
}
