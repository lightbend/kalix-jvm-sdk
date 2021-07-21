package customer;

import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityFactory;
import com.google.protobuf.Descriptors;
import customer.api.CustomerApi;

// to be generated in generated-source/src/main/java
public abstract class CustomerValueEntityFactoryBase implements ValueEntityFactory<CustomerValueEntity> {

    // depends on proto file definition therefore final
    @Override
    public final Descriptors.ServiceDescriptor serviceDescriptor() {
        return CustomerApi.getDescriptor().findServiceByName("CustomerService");
    }

    // depends on proto file definition therefore final
    @Override
    public final String entityTypeHint() {
        return "customers";
    }

    // depends on proto file definition therefore final
    @Override
    public final CustomerValueEntityHandler newHandler(ValueEntityContext context) {
        return new CustomerValueEntityHandler(newInstance(context));
    }

    // depends on proto file definition therefore final
    @Override
    public final Descriptors.FileDescriptor[] additionalDescriptors() {
        return new Descriptors.FileDescriptor[0];
    }
}
