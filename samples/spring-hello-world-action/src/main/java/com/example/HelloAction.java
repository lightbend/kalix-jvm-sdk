package com.example;


// tag::hello[]
import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class HelloAction extends Action { // <1>
  @GetMapping("/greeting/{name}") // <2>
  public Effect<String> hello(@PathVariable String name) {
    return effects().reply("Hello, " + name + "!"); // <3>
  }
}
// end::hello[]
