package $package;

import io.cloudstate.javasupport.*;
import ${package}.persistence.Domain;

public final class Main {
  public static final void main(String[] args) throws Exception {
    new CloudState()
        .registerEventSourcedEntity(
            MyServiceEntity.class,
            Myentity.getDescriptor().findServiceByName("MyService"),
            Domain.getDescriptor())
        .start()
        .toCompletableFuture()
        .get();
  }
}
