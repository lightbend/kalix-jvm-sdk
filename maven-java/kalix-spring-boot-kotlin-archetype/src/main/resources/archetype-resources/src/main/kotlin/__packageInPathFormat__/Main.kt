package ${package};

import kalix.javasdk.annotations.Acl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kalix.javasdk.JsonSupport

@SpringBootApplication
// Allow all other Kalix services deployed in the same project to access the components of this
// Kalix service, but disallow access from the internet. This can be overridden explicitly
// per component or method using annotations.
// Documentation at https://docs.kalix.io/java/access-control.html
@Acl(allow = [Acl.Matcher(service = "*")])
class Main {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(Main::class.java)

    init {
      // KotlinModule allows for serialization of Kotlin data classes
      // without the need of explicit Jackson annotations.
      // It's configured in the companion object to ensure that it happens before Kalix starts
      JsonSupport.getObjectMapper().registerModule(KotlinModule())
    }
  }
}

fun main(args: Array<String>) {
  Main.logger.info("Starting Kalix Application")
  runApplication<Main>(*args)
}