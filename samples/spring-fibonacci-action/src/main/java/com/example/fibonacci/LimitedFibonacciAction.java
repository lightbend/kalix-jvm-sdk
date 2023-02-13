package com.example.fibonacci;

import io.grpc.Status;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/limitedfibonacci")
public class LimitedFibonacciAction extends Action {

    private static final Logger logger = LoggerFactory.getLogger(LimitedFibonacciAction.class);

    private KalixClient kalixClient;

    private ActionCreationContext ctx;


    public LimitedFibonacciAction(ActionCreationContext ctx, KalixClient kalixClient) {
        this.ctx = ctx;
        this.kalixClient = kalixClient;
    }

    @GetMapping("/{number}/next")
    public Effect<Number> nextNumber(@PathVariable Long number) {
        if (number < 0 || number > 10000) {
            return effects().error("Only numbers between 0 and 10k are allowed", Status.Code.INVALID_ARGUMENT);
        } else {
            logger.info("Executing GET call to real /fibonacci = " + number);
            var serviceCall = kalixClient.get("/fibonacci/"+number+"/next", Number.class);

            return effects().forward(serviceCall);
        }
    }

    @PostMapping("/next")
    public Effect<Number> nextNumber(@RequestBody Number number) {
        if (number.value() < 0 || number.value() > 10000) {
            return effects().error("Only numbers between 0 and 10k are allowed", Status.Code.INVALID_ARGUMENT);
        } else {
            logger.info("Executing POST call to real /fibonacci = " + number.value());
            var serviceCall = kalixClient.post("/fibonacci/next", number, Number.class);

            return effects().forward(serviceCall);
        }
    }
}
