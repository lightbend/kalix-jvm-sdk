package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.example.domain.CounterHandler;
import com.example.domain.Counter;

public final class MainComponentRegistrations2 {
    
    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        return akkaServerless
                  .lowLevel()
                  .registerValueEntity(
                      ctx -> new CounterHandler(new Counter(ctx.entityId())),
                      CounterHandler.serviceDescriptor,
                      CounterHandler.entityType,
                      ValueEntityOptions.defaults());
    }
}
