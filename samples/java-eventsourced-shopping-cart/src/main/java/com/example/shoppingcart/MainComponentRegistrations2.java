package com.example.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.example.shoppingcart.domain.ShoppingCartImpl;
import com.google.protobuf.EmptyProto;

public final class MainComponentRegistrations2 {
    
    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        return akkaServerless
                .registerEventSourcedEntity(
                    ShoppingCartImpl.class,
                    ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService"),
                    ShoppingCartDomain.getDescriptor(),
                    EmptyProto.getDescriptor()
                );
    }
}
