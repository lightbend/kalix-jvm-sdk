package user.registry;

import kalix.javasdk.annotations.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    logger.info("Starting Kalix Application");
    SpringApplication.run(Main.class, args);
  }
}