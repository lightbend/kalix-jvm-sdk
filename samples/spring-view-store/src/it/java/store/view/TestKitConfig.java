package store.view;

import kalix.javasdk.testkit.KalixTestKit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

// tag::testkit-advanced-views[]
@TestConfiguration
public class TestKitConfig {
  @Bean
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT.withAdvancedViews();
  }
}
// end::testkit-advanced-views[]
