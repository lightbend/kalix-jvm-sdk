package com.example

import com.google.api.HttpBody
import com.google.protobuf.ByteString
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.Action.Effect
import kalix.scalasdk.action.ActionCreationContext
import org.slf4j.LoggerFactory

import java.nio.file.{Files, NoSuchFileException, Paths}
import scala.util.{Failure, Success, Try}

class FileServiceAction(ctx: ActionCreationContext) extends AbstractFileServiceAction {

  private val log = LoggerFactory.getLogger("com.example.FileServiceAction")
  private val defaultContentType = "text/html"
  private val basDir = "src/main/resources/web"

  private val contentTypes = Map(
    ".js" -> "text/javascript",
    ".css" -> "text/css",
    ".png" -> "image/png"
  )

  private def contentTypeByFile(fileName: String) =
    contentTypes
      .filter(suffix => fileName.endsWith(suffix._1))
      .map(_._2)
      .headOption
      .getOrElse(defaultContentType)

  private def loadFile(dir: String, file: String): Effect[HttpBody] = {
    val fullPath = s"$basDir$dir/$file"
    println(fullPath)
    Try {
      // tag::200-ok[]
      val byteArray = Files.readAllBytes(Paths.get(fullPath))
      val contentType = contentTypeByFile(file)
      log.info(s"Serving $fullPath with $contentType")
      val header = Metadata.empty.add("Cache-Control", "no-cache")
      effects.reply(new HttpBody(contentType, ByteString.copyFrom(byteArray)), header)
      // end::200-ok[]
    } match {
      case Failure(_: NoSuchFileException) =>
        log.debug(s"404: File $fullPath does not exist")
        // tag::404-not-found[]
        effects.ignore
      // end::404-not-found[]
      case Failure(exception) =>
        log.error(s"500: Not able to serve $fullPath", exception)
        // tag::500-error[]
        effects.error(s"Not able to serve $file")
        // end::500-error[]
      case Success(value) => value
    }
  }

  override def getFile(file: File): Effect[HttpBody] =
    loadFile("", file.file)

  override def getFileInDir(fileInDir: FileInDir): Effect[HttpBody] =
    loadFile(s"/${fileInDir.directory}", fileInDir.file)
}
