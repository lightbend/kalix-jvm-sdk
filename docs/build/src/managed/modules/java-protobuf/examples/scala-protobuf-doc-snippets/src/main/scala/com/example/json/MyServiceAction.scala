package com.example.json

import kalix.scalasdk.JsonSupport
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.empty.Empty
import org.slf4j.LoggerFactory

// tag::action[]
class MyServiceAction(creationContext: ActionCreationContext) extends AbstractMyServiceAction {

  private val log = LoggerFactory.getLogger(classOf[MyServiceAction])

  override def consume(any: ScalaPbAny): Action.Effect[Empty] = {
    val jsonMessage = JsonSupport.decodeJson[JsonKeyValueMessage](any) // <1>
    log.info("Consumed {}", jsonMessage)
    effects.reply(Empty.defaultInstance)
  }

  override def produce(keyValue: KeyValue): Action.Effect[ScalaPbAny] = {
    val jsonMessage = JsonKeyValueMessage(keyValue.key, keyValue.value) // <2>
    val jsonAny = JsonSupport.encodeJson(jsonMessage) // <3>
    effects.reply(jsonAny)
  }
}
// end::action[]