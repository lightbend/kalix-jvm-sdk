package com.example.trigger;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Trigger;
import org.springframework.web.bind.annotation.PostMapping;


// tag::hook[]
public class OnStartupAction extends Action { // <1>

  @PostMapping("/init")
  @Trigger.OnStartup( // <2>
      maxRetries = 3) // <3>
  public Action.Effect<String> init() { // <4>
    // Do some initial operations here
    return effects().reply("Done");
  }

}
// end::hook[]
