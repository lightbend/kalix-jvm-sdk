package wiring.action;

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import wiring.SomeComponent;

@RequestMapping("/echo")
public class EchoAction extends Action {

  private final ActionCreationContext context;
  private final SomeComponent someComponent;

  public EchoAction(ActionCreationContext context, SomeComponent someComponent) {
    this.context = context;
    this.someComponent = someComponent;
  }

  @GetMapping("/message/{msg}")
  public Effect<Message> echo(@PathVariable String msg) {
    return effects().reply(new Message(msg));
  }
}
