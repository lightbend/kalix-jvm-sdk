package com.example.wiring.actions.echo;

import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class ActionWithPrimitives extends Action {

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
}
