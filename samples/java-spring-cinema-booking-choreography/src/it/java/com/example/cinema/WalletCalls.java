package com.example.cinema;

import com.example.wallet.model.WalletApiModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

@Component
public class WalletCalls {

    @Autowired
    private WebClient webClient;

    private Duration timeout = Duration.ofSeconds(10);

    public void createWallet(String walletId, int amount) {
        ResponseEntity<Void> response = webClient.post().uri("/wallet/" + walletId + "/create/" + amount)
                .retrieve()
                .toBodilessEntity()
                .block(timeout);

        assertThat(response.getStatusCode()).isEqualTo(OK);
    }

    public WalletApiModel.WalletResponse getWallet(String walletId) {
        return webClient.get().uri("/wallet/" + walletId)
                .retrieve()
                .bodyToMono(WalletApiModel.WalletResponse.class)
                .block(timeout);
    }

    public void chargeWallet(String walletId, WalletApiModel.WalletCommand.ChargeWallet chargeWallet) {
        ResponseEntity<Void> response = webClient.patch().uri("/wallet/" + walletId + "/charge")
                .bodyValue(chargeWallet)
                .header("skip-failure-simulation", "true")
                .retrieve()
                .toBodilessEntity()
                .block(timeout);

        assertThat(response.getStatusCode()).isEqualTo(OK);
    }
}
