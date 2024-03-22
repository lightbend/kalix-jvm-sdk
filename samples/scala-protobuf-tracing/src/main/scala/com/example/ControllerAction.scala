package com.example

import com.google.protobuf.empty.Empty
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Scope
import kalix.scalasdk.action.{Action, ActionCreationContext}
import sttp.client4.quick._
import sttp.client4.{Response, _}

import scala.concurrent.Future

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.



class ControllerAction(creationContext: ActionCreationContext) extends AbstractControllerAction {

  val url = "https://jsonplaceholder.typicode.com/posts/1"

  override def callSyncEndpoint(empty: Empty): Action.Effect[MessageResponse] = {
    val tracerOpt = actionContext.getOpenTelemetryTracer
    var responseBody = ""
    tracerOpt match {
      case Some(tracer) =>
        val span = tracer
          .spanBuilder("b")
          .setParent(actionContext.metadata.traceContext.asOpenTelemetryContext)
          .startSpan()
        val scope: Scope = span.makeCurrent()
        try {
          val response = quickRequest.get(uri"$url").send()
          if(response.code.isSuccess) {
            span.setAttribute("result", response.body)
          } else {
            span.setStatus(StatusCode.ERROR,response.statusText)
          }
          responseBody = response.body
        } finally {
          span.end()
          scope.close()
        }
      case None =>
        val response = quickRequest.get(uri"$url").send()
        responseBody = response.body
    }
    effects.reply(MessageResponse(responseBody))
  }


  import sttp.client4.akkahttp._
  import sttp.client4.json4s._

  import scala.concurrent.ExecutionContext.global

  case class HttpBinResponse(origin:String, headers: Map[String, String])
  implicit val serialization = org.json4s.native.Serialization
  implicit val formats = org.json4s.DefaultFormats
  val backend: StreamBackend[Future, Any] = AkkaHttpBackend()
  override def callAsyncEndpoint(empty: Empty): Action.Effect[MessageResponse] = {
    val request = basicRequest.get(uri"$url").response(asJson[HttpBinResponse])
    val response: Future[Response[Either[ResponseException[String, Exception], HttpBinResponse]]] =
      request.send(backend)

    val responseMessage: Future[MessageResponse] = response.map { response =>
      response.body match {
        case Left(resEx) => MessageResponse(resEx.toString)
        case Right(value) => MessageResponse(value.origin)
      }
    }
    effects.asyncReply(responseMessage)
  }

  def callEndpoint(url: String): String = {
    val response: Response[String] = quickRequest.get(uri"$url").send()
    response.body
  }
}

