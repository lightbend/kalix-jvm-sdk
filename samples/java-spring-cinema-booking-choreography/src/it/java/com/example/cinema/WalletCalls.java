package com.example.cinema;

import com.example.common.Response;
import com.example.wallet.Wallet;
import com.example.wallet.WalletEntity;
import kalix.javasdk.client.ComponentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class WalletCalls {

    @Autowired
    private ComponentClient componentClient;

    private int timeout = 10;
    public void createWallet(String walletId, int amount) throws Exception{
        componentClient.forEventSourcedEntity(walletId).call(WalletEntity::create).params(walletId,amount).execute().toCompletableFuture().get(timeout, TimeUnit.SECONDS);
    }

    public Wallet.WalletResponse getWallet(String walletId) throws Exception{
        return componentClient.forEventSourcedEntity(walletId).call(WalletEntity::get).execute().toCompletableFuture().get(timeout, TimeUnit.SECONDS);
    }

    public void chargeWallet(String walletId, Wallet.WalletCommand.ChargeWallet chargeWallet) throws Exception{
        componentClient.forEventSourcedEntity(walletId).call(WalletEntity::charge).params(chargeWallet).execute().exceptionally(e -> Response.Failure.of(e.getMessage())).toCompletableFuture().get(timeout, TimeUnit.SECONDS);
    }
}
