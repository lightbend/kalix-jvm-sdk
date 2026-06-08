/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.telemetry

import java.io.OutputStreamWriter

import scala.util.Failure
import scala.util.Success

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.util.ByteString
import com.typesafe.config.Config
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import org.slf4j.LoggerFactory

object PrometheusExporter {

  final case class Settings(enabled: Boolean, host: String, port: Int)

  object Settings {
    def apply(rootConfig: Config): Settings = {
      val config = rootConfig.getConfig("kalix.telemetry.metrics")
      Settings(
        enabled = config.getBoolean("enabled"),
        host = config.getString("prometheus.host"),
        port = config.getInt("prometheus.port"))
    }
  }

  /**
   * Reads the settings from the system config and, if metrics are enabled, starts a Prometheus exporter HTTP server
   * exposing the (optionally JVM) metrics on the `/metrics` endpoint.
   */
  def start(system: ActorSystem): Unit = {
    val settings = Settings(system.settings.config)
    if (settings.enabled) {
      val registry = CollectorRegistry.defaultRegistry
      DefaultExports.register(registry)
      val exporter = new PrometheusExporter(registry, settings.host, settings.port)(system)
      exporter.start()
    }
  }
}

/**
 * Serves Prometheus metrics using Akka HTTP.
 */
class PrometheusExporter(registry: CollectorRegistry, host: String, port: Int)(implicit system: ActorSystem) {

  private val log = LoggerFactory.getLogger(classOf[PrometheusExporter])

  private[this] val PrometheusContentType = ContentType.parse(TextFormat.CONTENT_TYPE_004).toOption.get

  private def routes = get {
    (path("metrics") | pathSingleSlash) {
      encodeResponse {
        parameter(Symbol("name[]").*) { names =>
          complete {
            val namesSet = new java.util.HashSet[String]()
            names.foreach(namesSet.add)
            val builder = ByteString.newBuilder
            val writer = new OutputStreamWriter(builder.asOutputStream)
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(namesSet))
            // Very important to flush the writer before we build the byte string!
            writer.flush()
            HttpEntity(PrometheusContentType, builder.result())
          }
        }
      }
    }
  }

  def start(): Unit = {
    import system.dispatcher
    Http().newServerAt(host, port).bind(routes).onComplete {
      case Success(binding) =>
        log.info("Prometheus exporter started on {}", binding.localAddress)
      case Failure(ex) =>
        log.error("Error starting Prometheus exporter!", ex)
    }
  }
}
