package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.domain.CounterDomain;
import com.example.domain.CounterImpl;
import com.google.protobuf.EmptyProto;

public final class MainComponentRegistrations2 {
    
    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        return akkaServerless
                .registerValueEntity(
                    CounterImpl.class,
                    CounterApi.getDescriptor().findServiceByName("CounterService"),
                    CounterDomain.getDescriptor(),
                    EmptyProto.getDescriptor()
                );
    }
}
