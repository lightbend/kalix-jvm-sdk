package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.example.CounterApi;
import com.google.protobuf.Empty;

import java.util.Optional;

/** A value entity. */
public abstract class CounterInterface2 {
    
    public class CommandNotImplementedException extends UnsupportedOperationException {
        public CommandNotImplementedException() {
            super("You have either created a new command or removed the handling of an existing command. Please declare a method in your \"impl\" class for this command.");
        }
    }
    
    @CommandHandler(name = "Increase")
    public abstract Effect<Empty> increase(CounterApi.IncreaseValue command, Optional<CounterDomain.CounterState> currentState, CommandContext<CounterDomain.CounterState> ctx);

    @CommandHandler(name = "Decrease")
    public abstract Effect<Empty> decrease(CounterApi.DecreaseValue command, Optional<CounterDomain.CounterState> currentState, CommandContext<CounterDomain.CounterState> ctx);

    @CommandHandler(name = "Reset")
    public abstract Effect<Empty> reset(CounterApi.ResetValue command, Optional<CounterDomain.CounterState> currentState, CommandContext<CounterDomain.CounterState> ctx);

    @CommandHandler(name = "GetCurrentCounter")
    public abstract Effect<CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter command, Optional<CounterDomain.CounterState> currentState, CommandContext<CounterDomain.CounterState> ctx);

}
