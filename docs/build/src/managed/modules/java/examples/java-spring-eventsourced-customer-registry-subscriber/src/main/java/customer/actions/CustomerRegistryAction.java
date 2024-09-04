package customer.actions;

import kalix.javasdk.action.Action;
import kalix.spring.WebClientProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;

@RequestMapping("/customer/{customerId}")
public class CustomerRegistryAction extends Action {

  public record Address(String street, String city) {
  }

  public record Customer(String email, String name, Address address) {
  }

  public record Confirm(String msg) {
  }

  private final WebClient webClient;

  public CustomerRegistryAction(WebClientProvider webClientProvider) {
    this.webClient = webClientProvider.webClientFor("customer-registry");
  }


  @PostMapping("/create")
  public Effect<Confirm> create(@PathVariable String customerId, @RequestBody Customer customer) {
    // make call on customer-registry service
    var res =
      webClient.post()
        .uri("/customer/{customerId}/create", customerId)
        .bodyValue(customer)
        .retrieve()
        .bodyToMono(Confirm.class)
        .toFuture();

    return effects().asyncReply(res);
  }
}
