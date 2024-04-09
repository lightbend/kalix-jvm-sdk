/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.echo;

import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.stream.Collectors;

public class ActionWithPrimitives extends Action {

  @GetMapping("/optional-params-action")
  public Effect<Message> stringMessageWithOptionalParams(@RequestParam Long longValue,
                                                         @RequestParam(required = false) Integer intValue,
                                                         @RequestParam(required = false) String stringValue) {
    String response = String.valueOf(longValue) + String.valueOf(intValue) + stringValue;

    return effects().reply(new Message(response));
  }

  @GetMapping("/action/{doubleValue}/{floatValue}/{intValue}/{longValue}")
  public Effect<Message> stringMessage(@PathVariable double doubleValue,
                                       @PathVariable float floatValue,
                                       @PathVariable int intValue,
                                       @PathVariable long longValue,
                                       @RequestParam short shortValue,
                                       @RequestParam byte byteValue,
                                       @RequestParam char charValue,
                                       @RequestParam boolean booleanValue) {
    String response = String.valueOf(doubleValue) +
      floatValue +
      intValue +
      longValue +
      shortValue +
      byteValue +
      charValue +
      booleanValue;

    return effects().reply(new Message(response));
  }

  @GetMapping("/action_collections")
  public Effect<Message> listMessage(@RequestParam Collection<Integer> ints) {
    String response = ints.stream().map(Object::toString).collect(Collectors.joining(","));

    return effects().reply(new Message(response));
  }

}
