/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package customer.view;

// tag::process-events[]
import com.akkaserverless.javasdk.view.View;
import com.google.protobuf.Any;
import customer.domain.CustomerDomain;
import java.util.Optional;

@View // <1>
public class CustomerByNameView extends AbstractCustomerByNameView { // <2>

    @Override // <3>
    public CustomerDomain.CustomerState processCustomerCreated(
        CustomerDomain.CustomerCreated event, Optional<CustomerDomain.CustomerState> state) {
        if (state.isPresent()) {
            return state.get(); // already created
        } else {
            return event.getCustomer();
        }
    }

    @Override // <3>
    public CustomerDomain.CustomerState processCustomerNameChanged(
        CustomerDomain.CustomerNameChanged event, Optional<CustomerDomain.CustomerState> state) {
        if (state.isPresent()) {
            return state.get().toBuilder().setName(event.getNewName()).build();
        } else {
            throw new RuntimeException("Received `CustomerNameChanged`, but no state exists.");
        }
    }

    @Override // <3>
    public CustomerDomain.CustomerState ignoreOtherEvents(
        Any event, Optional<CustomerDomain.CustomerState> state) {
        return state.orElseThrow(
            () ->
                new RuntimeException(
                    "Received `" + event.getClass().getSimpleName() + "`, but no state exists."));
    }
}
// end::process-events[]
