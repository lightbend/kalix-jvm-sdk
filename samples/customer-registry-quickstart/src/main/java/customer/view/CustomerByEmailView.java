/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package customer.view;

import com.akkaserverless.javasdk.view.View;
import com.akkaserverless.javasdk.view.ViewContext;
import customer.domain.CustomerDomain;
import java.util.function.Function;

public class CustomerByEmailView extends AbstractCustomerByEmailView {

  public CustomerByEmailView(ViewContext context) {}

  @Override
  public CustomerDomain.CustomerState emptyState() {
    throw new RuntimeException("Empty state for 'CustomerByEmailView' not implemented yet");
  }

  
}