package customer;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedContext;
import com.google.protobuf.AnyProto;
import customer.domain.CustomerDomain;
import customer.domain.CustomerEntity;
import customer.domain.CustomerEntityProvider;
import customer.view.CustomerByNameView;
import customer.view.CustomerViewModel;

import java.util.function.Function;

public class AkkaServerlessFactory {

 public static AkkaServerless withComponents(
     Function<EventSourcedContext, CustomerEntity> createCustomerEntity) {
  AkkaServerless akkaServerless = new AkkaServerless();
  return akkaServerless
      .register(CustomerEntityProvider.of(createCustomerEntity));
 }

}
