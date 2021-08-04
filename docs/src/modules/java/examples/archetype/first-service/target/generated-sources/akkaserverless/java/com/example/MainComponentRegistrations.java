package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.domain.CounterImpl;
import com.example.domain.CounterDomain;

public final class MainComponentRegistrations {
    
    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        return akkaServerless
                .registerValueEntity(
                    CounterImpl.class,
                    CounterApi.getDescriptor().findServiceByName("CounterService"),
                    CounterDomain.getDescriptor()
                );
    }
}