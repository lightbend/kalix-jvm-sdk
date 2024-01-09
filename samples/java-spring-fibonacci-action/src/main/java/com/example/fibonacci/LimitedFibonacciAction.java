package com.example.fibonacci;

import com.google.protobuf.any.Any;
import io.grpc.Status;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/limitedfibonacci")
public class LimitedFibonacciAction extends Action {

    private static final Logger logger = LoggerFactory.getLogger(LimitedFibonacciAction.class);
    // tag::injecting-component-client[]
    private ComponentClient componentClient; // <1>

    public LimitedFibonacciAction(ComponentClient componentClient) { // <2>
        this.componentClient = componentClient; // <3>
    }
    // end::injecting-component-client[]

    @GetMapping("/{number}/next")
    public Effect<Number> nextNumberPath(@PathVariable Long number) {
        if (number < 0 || number > 10000) {
            return effects().error("Only numbers between 0 and 10k are allowed", Status.Code.INVALID_ARGUMENT);
        } else {
            logger.info("Executing GET call to real /fibonacci = " + number);
            // tag::component-client[]
            DeferredCall<Any, Number> deferredCall = componentClient.forAction() // <1>
              .call(FibonacciAction::getNumber) // <2>
              .params(number); // <3>

            return effects().forward(deferredCall);
            // end::component-client[]
        }
    }

    @PostMapping("/next")
    public Effect<Number> nextNumber(@RequestBody Number number) {
        if (number.value() < 0 || number.value() > 10000) {
            return effects().error("Only numbers between 0 and 10k are allowed", Status.Code.INVALID_ARGUMENT);
        } else {
            logger.info("Executing POST call to real /fibonacci = " + number.value());
            var serviceCall = componentClient.forAction().call(FibonacciAction::nextNumber).params(number);

            return effects().forward(serviceCall);
        }
    }
}
