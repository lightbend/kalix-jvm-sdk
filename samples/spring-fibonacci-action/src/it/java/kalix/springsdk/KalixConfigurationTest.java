package kalix.springsdk;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.springsdk.impl.KalixServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

// TODO: we need a better TestKit integration for the Spring SDK
// for now, this server us to prove that the Spring wiring is working as expect
@Import(KalixConfiguration.class)
@TestConfiguration
public class KalixConfigurationTest {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private KalixConfiguration kalixConfiguration;

  @Bean
  public KalixServer kalixServer() {
    return new KalixServer(applicationContext, kalixConfiguration.config());
  }

  /**
   * WebClient pointing to the proxy.
   */
  @Bean
  public WebClient createWebClient(KalixTestKit kalixTestKit) {
    return WebClient.builder().baseUrl("http://localhost:" + kalixTestKit.getPort())
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .build();
  }


  @Bean
  public KalixTestKit kalixTestKit() {
    logger.info("Starting Kalix TestKit...");
    KalixTestKit kalixTestKit = new KalixTestKit(kalixServer().kalix());
    kalixTestKit.start(kalixConfiguration.config());
    logger.info("Kalix Proxy running on port: " + kalixTestKit.getPort());
    return kalixTestKit;
  }


}
