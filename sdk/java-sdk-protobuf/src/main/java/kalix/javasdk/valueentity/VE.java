package kalix.javasdk.valueentity;

import io.grpc.Status;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.SideEffect;
import kalix.javasdk.StatusCode;

import java.util.Collection;

public abstract class VE<S> {

  public interface Effect2<T> {
    public void nothing();
  }
}
