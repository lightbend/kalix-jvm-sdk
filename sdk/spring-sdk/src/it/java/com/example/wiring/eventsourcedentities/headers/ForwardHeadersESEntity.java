package com.example.wiring.eventsourcedentities.headers;

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.ForwardHeaders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.example.wiring.actions.headers.ForwardHeadersAction.SOME_HEADER;

@EntityKey("id")
@EntityType("forward-headers-es")
@RequestMapping("/forward-headers-es/{id}")
@ForwardHeaders(SOME_HEADER)
public class ForwardHeadersESEntity extends EventSourcedEntity<String> {

  @PutMapping()
  public Effect<Message> createUser() {
    String headerValue = commandContext().metadata().get(SOME_HEADER).orElse("");
    return effects().reply(new Message(headerValue));
  }
}
