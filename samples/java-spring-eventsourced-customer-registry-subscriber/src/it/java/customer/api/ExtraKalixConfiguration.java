package customer.api;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kalix.devtools.impl.DockerComposeUtils;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.boot.KalixConfiguration;
import kalix.spring.boot.KalixConfigurationTest;
import kalix.spring.impl.KalixSpringApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration(after = KalixConfigurationTest.class )
public class ExtraKalixConfiguration {

  @Autowired
  ApplicationContext applicationContext;

  public ExtraKalixConfiguration() {
    System.out.println("--------> ExtraConfiguration() called");
  }

  @Bean
  public Config config() {
    System.out.println("--------> config() called - config redefined");
    Map<String, Object> confMap = new HashMap<>();
//    confMap.put("kalix.user-function-port", dockerComposeUtils.readUserFunctionPort());
    // don't kill the test JVM when terminating the KalixRunner
    confMap.put("kalix.system.akka.coordinated-shutdown.exit-jvm", "off");
    // dev-mode should be false when running integration tests
    confMap.put("kalix.dev-mode.enabled", false);
    // TODO: read service-port-mappings and pass to UF
    return ConfigFactory.parseMap(confMap).withFallback(ConfigFactory.load());
  }

  @Bean
  public KalixSpringApplication kalixSpringApplication(Config config) {
    System.out.println("--------> kalixSpringApplication() called - kalixSpringApplication redefined");
    return new KalixSpringApplication(applicationContext, config);
  }

  @Bean
  public DockerComposeUtils dockerComposeUtils() {
    return new DockerComposeUtils("docker-compose-integration.yml");
  }
}
