package com.example.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kalix.javasdk.Metadata;
import kalix.javasdk.client.ComponentClient;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class JwtIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private ComponentClient componentClient;

  // tag::bearer-token-claims-test[]
  @Test
  public void testMsgWithClaim() throws Exception {
    String bearerToken = bearerTokenWith( // <1>
        Map.of("iss", "my-issuer", "sub", "hello"));

    var msg = "Hello from integration test";
    var response = componentClient
        .forAction().call(JWTAction::messageWithClaimValidation).params(msg)
        .withMetadata( // <2>
            Metadata.EMPTY.add("Authorization", "Bearer " + bearerToken))
        .execute()
        .toCompletableFuture()
        .get();

    assertThat(response).contains(msg);
  }

  private String bearerTokenWith(Map<String, String> claims) throws JsonProcessingException {
    // setting algorithm to none
    String alg = Base64.getEncoder().encodeToString("""
        {
          "alg": "none"
        }
        """.getBytes()); // <3>
    byte[] jsonClaims = new ObjectMapper().writeValueAsBytes(claims);

    // no validation is done for integration tests, thus no valid signature required
    return alg + "." +  Base64.getEncoder().encodeToString(jsonClaims); // <4>
  }
  // end::bearer-token-claims-test[]
}
