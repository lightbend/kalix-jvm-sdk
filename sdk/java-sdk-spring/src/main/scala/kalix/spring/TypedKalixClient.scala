package kalix.spring

import akka.japi.function.Function3
import kalix.javasdk.impl.client.ServiceCall2
import kalix.javasdk.valueentity.VE
import kalix.javasdk.valueentity.ValueEntity

class TypedKalixClient(kalixClient: KalixClient) {

  def ref2[T, A1, A2, R](methodRef: Function3[T, A1, A2, ValueEntity.Effect[R]]): ServiceCall2[A1, A2, R] = {
    ???
  }

  def ref[R, A1, A2](lambda: VE.Effect2[R]): ServiceCall2[A1, A2, R] = {
    ???
  }
}
