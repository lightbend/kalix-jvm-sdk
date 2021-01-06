package $package;

import io.cloudstate.javasupport.*;
import ${package}.Myentity;
import ${package}.persistence.Domain;
import static java.util.Collections.singletonMap;

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
