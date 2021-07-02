package com.example.domain;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.valueentity.*;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/** A value entity. */
public abstract class CounterInterface {
    
    public class CommandNotImplementedException extends UnsupportedOperationException {
        public CommandNotImplementedException() {
            super("You have either created a new command or removed the handling of an existing command. Please declare a method in your \"impl\" class for this command.");
        }
    }
    
    @CommandHandler(name = "Increase")
    public Reply<Empty> increaseWithReply(CounterApi.IncreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return Reply.message(increase(command, ctx));
    }
    
    protected Empty increase(CounterApi.IncreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return increase(command);
    }
    
    protected Empty increase(CounterApi.IncreaseValue command) {
        throw new CommandNotImplementedException();
    }
    
    @CommandHandler(name = "Decrease")
    public Reply<Empty> decreaseWithReply(CounterApi.DecreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return Reply.message(decrease(command, ctx));
    }
    
    protected Empty decrease(CounterApi.DecreaseValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return decrease(command);
    }
    
    protected Empty decrease(CounterApi.DecreaseValue command) {
        throw new CommandNotImplementedException();
    }
    
    @CommandHandler(name = "Reset")
    public Reply<Empty> resetWithReply(CounterApi.ResetValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return Reply.message(reset(command, ctx));
    }
    
    protected Empty reset(CounterApi.ResetValue command, CommandContext<CounterDomain.CounterState> ctx) {
        return reset(command);
    }
    
    protected Empty reset(CounterApi.ResetValue command) {
        throw new CommandNotImplementedException();
    }
    
    @CommandHandler(name = "GetCurrentCounter")
    public Reply<CounterApi.CurrentCounter> getCurrentCounterWithReply(CounterApi.GetCounter command, CommandContext<CounterDomain.CounterState> ctx) {
        return Reply.message(getCurrentCounter(command, ctx));
    }
    
    protected CounterApi.CurrentCounter getCurrentCounter(CounterApi.GetCounter command, CommandContext<CounterDomain.CounterState> ctx) {
        return getCurrentCounter(command);
    }
    
    protected CounterApi.CurrentCounter getCurrentCounter(CounterApi.GetCounter command) {
        throw new CommandNotImplementedException();
    }
}