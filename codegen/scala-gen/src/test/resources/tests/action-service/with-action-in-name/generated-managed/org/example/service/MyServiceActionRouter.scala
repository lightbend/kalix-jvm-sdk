package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk.impl.action.ActionRouter.HandlerNotFound
import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.MessageEnvelope
import com.akkaserverless.scalasdk.impl.action.ActionRouter

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An Action handler */
class MyServiceActionRouter(action: MyServiceActionImpl) extends ActionRouter[MyServiceActionImpl](action) {

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]):  Action.Effect[_] = {
    commandName match {
      case "simpleMethod" =>
        action.simpleMethod(message.payload.asInstanceOf[MyRequest])

      case _ =>
        throw new HandlerNotFound(commandName)
    }
  }

  override def handleStreamedOut(commandName: String, message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {
    commandName match {

      case _ =>
        throw new HandlerNotFound(commandName)
    }
  }

  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] = {
    commandName match {

      case _ =>
        throw new HandlerNotFound(commandName)
    }
  }

  override def handleStreamed(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] = {
    commandName match {

      case _ =>
        throw new HandlerNotFound(commandName)
    }
  }
}

