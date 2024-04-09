/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import akka.actor.ActorSystem
import akka.actor.ClassicActorSystemProvider
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import kalix.protocol.discovery.IdentificationInfo
import kalix.protocol.discovery.ProxyInfo
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicReference

object ProxyInfoHolder extends ExtensionId[ProxyInfoHolder] with ExtensionIdProvider {
  override def get(system: ActorSystem): ProxyInfoHolder = super.get(system)

  override def get(system: ClassicActorSystemProvider): ProxyInfoHolder = super.get(system)

  override def createExtension(system: ExtendedActorSystem): ProxyInfoHolder =
    new ProxyInfoHolder

  override def lookup: ExtensionId[_ <: Extension] = this
}

class ProxyInfoHolder extends Extension {

  private val log = LoggerFactory.getLogger(classOf[ProxyInfoHolder])

  private val _proxyHostname = new AtomicReference[String]()
  private val _proxyPort = new AtomicReference[Int](-1)
  @volatile private var _identificationInfo: Option[IdentificationInfo] = None
  @volatile private var _proxyTracingCollectorEndpoint: Option[String] = None

  def setProxyInfo(proxyInfo: ProxyInfo): Unit = {

    val chosenProxyName =
      if (proxyInfo.internalProxyHostname.isEmpty) {
        // for backward compatibility with proxy 1.0.14 or older
        proxyInfo.proxyHostname
      } else {
        proxyInfo.internalProxyHostname
      }

    // don't set if already overridden (by testkit)
    _proxyHostname.compareAndSet(null, chosenProxyName)
    _proxyPort.compareAndSet(-1, proxyInfo.proxyPort)
    _identificationInfo = proxyInfo.identificationInfo
    _proxyTracingCollectorEndpoint = Some(proxyInfo.tracingCollectorEndpoint)

    log.debug("Runtime hostname: [{}]", chosenProxyName)
    log.debug("Runtime port to: [{}]", proxyInfo.proxyPort)
    log.debug("Identification name: [{}]", proxyInfo.identificationInfo)
    log.debug("Runtime Tracing collector endpoint: [{}]", proxyInfo.tracingCollectorEndpoint)
  }

  def proxyHostname: Option[String] = Option(_proxyHostname.get())

  def proxyTracingCollectorEndpoint: Option[String] = _proxyTracingCollectorEndpoint

  def identificationInfo: Option[IdentificationInfo] = _identificationInfo

  def proxyPort: Option[Int] = {
    // If portOverride is filled, we choose it. Otherwise we use the announced one.
    // Note: with old proxy versions `proxyInfo.proxyPort` will default to 0
    val chosenPort = _proxyPort.get()

    // We should never return the default Int 0, so we make it a None
    // This can happen if somehow this method called before we receive the ProxyInfo
    // or if an old version of the proxy is being used
    if (chosenPort != 0) Some(chosenPort)
    else None
  }

  def localIdentificationHeader: Option[(String, String)] =
    identificationInfo.collect {
      case IdentificationInfo(header, token, _, _, _) if header.nonEmpty && token.nonEmpty => (header, token)
    }

  def remoteIdentificationHeader: Option[(String, String)] =
    identificationInfo.collect {
      case IdentificationInfo(_, _, header, name, _) if header.nonEmpty && name.nonEmpty => (header, name)
    }

  /**
   * Change port disregarding what is announced in ProxyInfo This is required for the testkit because the port to use is
   * defined by testcontainers
   *
   * INTERNAL API
   */
  private[kalix] def overridePort(port: Int): Unit =
    _proxyPort.set(port)

  /**
   * INTERNAL API
   */
  private[kalix] def overrideProxyHost(host: String): Unit =
    _proxyHostname.set(host)

  private[kalix] def overrideTracingCollectorEndpoint(endpoint: String): Unit =
    _proxyTracingCollectorEndpoint = Some(endpoint)
}
