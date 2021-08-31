/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package customer.view;

import com.akkaserverless.javasdk.view.ViewCreationContext;
import customer.domain.CustomerDomain;

/** A view. */
public class CustomerByNameView extends AbstractCustomerByNameView {

    public CustomerByNameView(ViewCreationContext context) {
    }

    @Override
    public CustomerDomain.CustomerState emptyState() {
        return null;
    }

    @Override
    public UpdateEffect<CustomerDomain.CustomerState> updateCustomer(CustomerDomain.CustomerState state, CustomerDomain.CustomerState customerState) {
        return updateEffects().updateState(customerState);
    }
}
