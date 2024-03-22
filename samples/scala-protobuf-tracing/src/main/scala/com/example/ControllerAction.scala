package com.example

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.google.protobuf.empty.Empty
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import sttp.client4.quick._
import sttp.client4.Response

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using.Releasable
import scala.util.{Failure, Success, Try, Using}

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
        var span:Span = null;
        val trySpan: Try[Span] = Using.Manager { use =>
          use(span = tracer
            .spanBuilder("b")
            .setParent(actionContext.metadata.traceContext.asOpenTelemetryContext)
            .startSpan())(ReleaseSpan)
         use(span.makeCurrent())
          val response = quickRequest.get(uri"$url").send()
          responseBody = response.body
          span.setAttribute("result", response.body)
        }
        trySpan match {
          case Success(span) => {}
          case Failure(exception) => responseBody = exception.getMessage
        }

      case None => {
        val response = quickRequest.get(uri"$url").send()
        responseBody = response.body
      }

    }
    effects.reply(MessageResponse(responseBody))

    //effects.asyncEffect(responseFuture)
  }
  override def callAsyncEndpoint(empty: Empty): Action.Effect[MessageResponse] = {
    val response: Response[String] = quickRequest.get(uri"$url").send()
    response.body
  }

   def callEndpoint(url: String): String = {
    val response: Response[String] = quickRequest.get(uri"$url").send()
    response.body
  }

  object ReleaseSpan extends Releasable[Span] {
    /** Releases the specified resource. */
    override def release(resource: Span): Unit = resource.end()
  }
}

