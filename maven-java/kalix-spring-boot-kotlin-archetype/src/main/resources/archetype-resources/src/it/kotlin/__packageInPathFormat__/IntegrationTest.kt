package ${package}

import ${package}.Main
import kalix.spring.testkit.KalixIntegrationTestKitSupport

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Java SDK.
 *
 * This test will initiate a Kalix Runtime using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Main::class])
public class IntegrationTest : KalixIntegrationTestKitSupport() {

  @Autowired
  private val webClient: WebClient? = null

  @Test
  @Throws(Exception::class)
  fun test() {
    // implement your integration tests here by calling your
    // REST endpoints using the provided WebClient
  }
}