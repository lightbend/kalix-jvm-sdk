package com.example

import com.example.domain.Post
import com.google.protobuf.empty.Empty
import io.opentelemetry.api.trace.StatusCode
import kalix.scalasdk.action.{Action, ActionCreationContext}
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import sttp.client4._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ControllerAction(creationContext: ActionCreationContext) extends AbstractControllerAction {

  implicit val formats: Formats = DefaultFormats

  val url = "https://jsonpilaceholder.typicode.com/posts"

  // tag::create-close-span[]
  override def callAsyncEndpoint(empty: Empty): Action.Effect[MessageResponse] = {
    // tag::get-tracer[]
    val tracer = actionContext.getTracer
    // end::get-tracer[]
    val span = tracer.spanBuilder(s"$url/{}")
      .setParent(actionContext.metadata.traceContext.asOpenTelemetryContext)// <1>
      .startSpan()// <2>
      .setAttribute("post", "1")// <3>

      val responseBody: Future[MessageResponse] = callAsync()
      responseBody.onComplete {
        case Failure(exception) =>
          span
            .setStatus(StatusCode.ERROR, exception.getMessage)// <4>
            .end()// <5>
        case Success(response) =>
          span
            .setAttribute("result", response.message)// <3>
            .end()// <5>
      }
     effects.asyncReply(responseBody)
  }
  // end::create-close-span[]


  private def callAsync(): Future[MessageResponse] = {
    val request = quickRequest.get(uri"${url}/1")
    val response = request.send(Main.backend)
    response.map { response =>
      import org.json4s.jvalue2extractable
      val post = JsonMethods.parse(response.body).extract[Post]
      MessageResponse(post.title)
    }
  }
}

