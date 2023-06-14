package kalix.javasdk.action;

import akka.japi.function.Function3;
import kalix.javasdk.impl.client.ServiceCall2;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.spring.KalixClient;

public class JavaTypedKalixClient {

  private final KalixClient kalixClient;

  public JavaTypedKalixClient(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public <T, A1, A2, R> ServiceCall2<A1, A2, R> ref2(Function3<T, A1, A2, ValueEntity.Effect<R>> methodRef) {
    return new ServiceCall2<>(kalixClient, methodRef);
  }
}
