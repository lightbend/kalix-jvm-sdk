package customer;

import kalix.javasdk.JsonSupport;
import kalix.javasdk.annotations.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES;

@SpringBootApplication
// NOTE: This default ACL settings is very permissive as it allows any traffic from the internet.
// Our samples default to this permissive configuration to allow users to easily try it out.
// However, this configuration is not intended to be reproduced in production environments.
// Documentation at https://docs.kalix.io/java/access-control.html
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
// tag::object-mapper[]
public class Main {
  // end::object-mapper[]

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  // tag::object-mapper[]
  public static void main(String[] args) {
    // end::object-mapper[]
    logger.info("Starting Kalix Application");
    // tag::object-mapper[]
    JsonSupport.getObjectMapper()
      .configure(FAIL_ON_NULL_CREATOR_PROPERTIES, true); // <1>
    SpringApplication.run(Main.class, args);
  }
}
// end::object-mapper[]