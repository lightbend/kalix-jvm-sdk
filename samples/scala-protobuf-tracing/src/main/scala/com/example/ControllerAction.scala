package com.example

import com.google.protobuf.empty.Empty
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Scope
import kalix.scalasdk.action.{ Action, ActionCreationContext }

import sttp.client4.quick.RichRequest
import sttp.client4.{ Response, _ }

import scala.concurrent.Future

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
          if (response.code.isSuccess) {
            span.setAttribute("result", response.body)
          } else {
            span.setStatus(StatusCode.ERROR, response.statusText)
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

  override def callAsyncEndpoint(empty: Empty): Action.Effect[MessageResponse] = {
    val request = basicRequest.get(uri"$url")
    val response: Future[Response[Either[String, String]]] =
      request.send(Main.backend)
    val responseMessage: Future[MessageResponse] = response.map { response =>
      response.body match {
        case Left(resEx) =>
          MessageResponse(resEx)
        case Right(value) =>
          MessageResponse(value)
      }
    }
    effects.asyncReply(responseMessage)
  }
}
