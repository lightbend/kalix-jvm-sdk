/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package customer.view;

import com.akkaserverless.javasdk.view.View;
import com.akkaserverless.javasdk.view.ViewContext;
import customer.domain.CustomerDomain;
import java.util.function.Function;

public class CustomerByNameView extends AbstractCustomerByNameView {

  public CustomerByNameView(ViewContext context) {}

  @Override
  public CustomerDomain.CustomerState emptyState() {
    // Not actually invoked when not using "transform_updates: true"
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
  }

  
}