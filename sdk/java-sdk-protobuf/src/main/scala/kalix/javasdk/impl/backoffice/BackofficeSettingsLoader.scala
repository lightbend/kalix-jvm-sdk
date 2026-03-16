/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.backoffice

import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.grpc.GrpcClientSettings
import com.typesafe.config.Config
import kalix.api.projects.v1alpha.projects._
import kalix.javasdk.impl.GrpcClients
import kalix.protocol.discovery.{ BackofficeService, BackofficeSettings }
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._
import scala.util.Try
import scala.util.control.NonFatal

@InternalApi
private[impl] object BackofficeSettingsLoader {

  private val log = LoggerFactory.getLogger(getClass)

  /** This should only be called in dev-mode */
  def loadBackofficeSettings(system: ActorSystem): Future[Option[BackofficeSettings]] = {
    val config = system.settings.config.getConfig("kalix.dev-mode.backoffice")
    val servicesConfig = config.getObject("services")
    val enabled = config.getBoolean("enabled")
    val cliPath = config.getString("cli-path")
    val akkaPath = if (cliPath.nonEmpty) {
      cliPath
    } else if (sys.props("os.name").contains("Windows")) {
      "akka.exe"
    } else {
      "akka"
    }

    // Check if there is a backoffice service configuration, if there's not, there's no need to do any of the rest
    // of the work
    if (enabled && !servicesConfig.isEmpty) {
      val refreshTokenFromConfig = Some(config.getString("refresh-token")).filter(_.nonEmpty)
      val apiServerFromConfig = Some(config.getString("api-server")).filter(_.nonEmpty)
      val cliContext = Some(config.getString("cli-context")).filter(_.nonEmpty)
      val requestTimeout = config.getDuration("request-timeout").toScala

      val refreshToken = refreshTokenFromConfig
        .orElse(getCliConfig(akkaPath, cliContext, "refresh-token"))
        .getOrElse {
          sys.error(
            "Akka CLI is not logged in, either log in, or set refresh-token in application config or using AKKA_REFRESH_TOKEN environment variable.")
        }

      // Ignore errors looking up service host from CLI, ensures you can authenticate by passing AKKA_REFRESH_TOKEN
      // environment variable without the CLI installed, and we'll just default to api.kalix.io:443.
      val apiServer = apiServerFromConfig
        .orElse(Try(getCliConfig(akkaPath, cliContext, "api-server-host")).toOption.flatten)
        .getOrElse("api.kalix.io:443")
      val (apiServerHost, apiServerPort) = apiServer.split(":", 2) match {
        case Array(host)       => (host, 443)
        case Array(host, port) => (host, port.toInt)
        case _                 => (apiServer, 443)
      }

      val accessTokenCache = BackofficeAccessTokenCache(system)
      accessTokenCache.init(apiServerHost, apiServerPort, refreshToken)
      val accessToken = Await.result(accessTokenCache.accessToken(), requestTimeout)
      val projectsClient =
        ProjectsClient(GrpcClientSettings.connectToServiceAt(apiServerHost, apiServerPort)(system))(system)

      import system.dispatcher

      // Only load projects if we need them
      lazy val projectsFuture = loadProjects(projectsClient, accessToken, requestTimeout)
      val projectRegions = TrieMap.empty[String, Seq[Region]]

      servicesConfig
        .keySet()
        .asScala
        .foldLeft(Future.successful(BackofficeSettings(refreshToken = refreshToken, apiServer = apiServer))) {
          (future, service) =>
            val serviceConfig = config.getConfig("services").getConfig(service)
            val projectIdOrFriendlyName = serviceConfig.getString("project")
            for {
              settings <- future
              projectName <-
                if (isUuid(projectIdOrFriendlyName)) {
                  Future.successful(s"projects/$projectIdOrFriendlyName")
                } else {
                  projectsFuture.map(_.filter(_.friendlyName == projectIdOrFriendlyName) match {
                    case Nil =>
                      sys.error(s"Could not find project with friendly name $projectIdOrFriendlyName")
                    case Seq(single) =>
                      single.name
                    case multiple =>
                      val orgIdOrFriendlyName = getOpt(serviceConfig, "organization")
                        .getOrElse(sys.error(
                          s"organization is needed for backoffice service $service because there are multiple projects with a friendly name of $projectIdOrFriendlyName"))
                      multiple.find(_.owner.organizationOwner.exists(org =>
                        org.id == orgIdOrFriendlyName || org.friendlyName == orgIdOrFriendlyName)) match {
                        case Some(project) => project.name
                        case None =>
                          sys.error(
                            s"Could not find project with friendly name $projectIdOrFriendlyName owned by organization $orgIdOrFriendlyName")
                      }
                  })
                }
              regions <- projectRegions.get(projectName) match {
                case Some(regions) =>
                  Future.successful(regions)
                case None =>
                  loadRegions(projectsClient, accessToken, requestTimeout, projectName).map { regions =>
                    projectRegions.update(projectName, regions)
                    regions
                  }
              }
              region = regions match {
                case Nil =>
                  sys.error(s"Project $projectIdOrFriendlyName has no regions")
                case Seq(region) =>
                  region
                case multiple =>
                  getOpt(serviceConfig, "region") match {
                    case None =>
                      regions.find(_.primary).getOrElse {
                        sys.error(s"Project $projectIdOrFriendlyName has no primary region")
                      }
                    case Some(regionName) =>
                      val name = s"$projectName/regions/$regionName"
                      multiple.find(_.name == name) match {
                        case None =>
                          sys.error(s"Region $regionName not found for project $projectIdOrFriendlyName")
                        case Some(region) =>
                          region
                      }
                  }
              }
            } yield {
              val regionName = region.name.stripPrefix(s"$projectName/regions/")
              val serviceName = getOpt(serviceConfig, "service-name").getOrElse(service)
              val projectId = projectName.stripPrefix("projects/")

              log.info(s"Resolved service $service to use service $service in project $projectId in region $regionName")

              settings.copy(services = settings.services :+ BackofficeService(
                name = service,
                serviceName = serviceName,
                projectId = projectId,
                regionName = regionName,
                backofficeProxyHost = region.backofficeProxyHostname))
            }
        }
        .map { settings =>
          GrpcClients(system).provideBackofficeSettings(settings)
          Some(settings)
        }
    } else {
      Future.successful(None)
    }
  }

  private val UuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r
  private def isUuid(value: String) =
    UuidRegex.matches(value)

  private def getOpt(config: Config, key: String) = {
    if (config.hasPath(key)) {
      Some(config.getString(key))
    } else {
      None
    }
  }

  private def loadProjects(projectsClient: ProjectsClient, accessToken: String, requestTimeout: FiniteDuration)(implicit
      ec: ExecutionContext): Future[List[Project]] = {

    def doLoadProjects(pageToken: String, projects: List[Project]): Future[List[Project]] = {
      projectsClient
        .listProjects()
        .setDeadline(requestTimeout)
        .addHeader("Authorization", s"Bearer $accessToken")
        .invoke(ListProjectsRequest(pageToken = pageToken))
        .flatMap { response =>
          if (response.nextPageToken.isEmpty) {
            Future.successful(projects ++ response.projects)
          } else {
            doLoadProjects(response.nextPageToken, projects ++ response.projects)
          }
        }
    }

    doLoadProjects("", Nil)
  }

  private def loadRegions(
      projectsClient: ProjectsClient,
      accessToken: String,
      requestTimeout: FiniteDuration,
      projectName: String)(implicit ec: ExecutionContext): Future[List[Region]] = {
    def doLoadRegions(pageToken: String, regions: List[Region]): Future[List[Region]] = {
      projectsClient
        .listRegions()
        .setDeadline(requestTimeout)
        .addHeader("Authorization", s"Bearer $accessToken")
        .invoke(ListRegionsRequest(parent = projectName, pageToken = pageToken))
        .flatMap { response =>
          if (response.nextPageToken.isEmpty) {
            Future.successful(regions ++ response.regions)
          } else {
            doLoadRegions(response.nextPageToken, regions ++ response.regions)
          }
        }
    }

    doLoadRegions("", Nil)
  }

  private def getCliConfig(path: String, context: Option[String], setting: String): Option[String] = {
    executeCli(path, context, Seq("config", "get") :+ setting).trim match {
      case ""    => None
      case value => Some(value)
    }
  }

  private def executeCli(path: String, context: Option[String], args: Seq[String]): String = {
    import sys.process._
    val contextArgs = context.toSeq.flatMap { ctx =>
      Seq("--context", ctx)
    }
    val stderr = new StringBuilder

    try {
      Process(Seq(path) ++ contextArgs ++ args)
        .!!(ProcessLogger(line => stderr.append(line).append("\n")))
    } catch {
      case NonFatal(e) =>
        val stderrStr = stderr.result()
        if (stderrStr.nonEmpty) {
          throw new RuntimeException(s"Error executing Akka CLI: ${stderrStr.trim}", e)
        } else {
          throw new RuntimeException("Error executing Akka CLI", e)
        }
    }
  }

}
