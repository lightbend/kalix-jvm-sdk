package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.domain.CounterDomain;
import com.example.domain.Counter;
import com.google.protobuf.EmptyProto;

public final class MainComponentRegistrations2 {
    
    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        return akkaServerless
                .registerValueEntity(
                    Counter.class,
                    CounterApi.getDescriptor().findServiceByName("CounterService"),
                    CounterDomain.getDescriptor(),
                    EmptyProto.getDescriptor()
                );
    }
}
