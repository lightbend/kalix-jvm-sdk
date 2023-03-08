package store.view;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.springsdk.testkit.KalixConfigurationTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// tag::testkit-advanced-views[]
@Import(KalixConfigurationTest.class)
@TestConfiguration
public class TestKitConfig {
  @Bean
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT.withAdvancedViews();
  }
}
// end::testkit-advanced-views[]
