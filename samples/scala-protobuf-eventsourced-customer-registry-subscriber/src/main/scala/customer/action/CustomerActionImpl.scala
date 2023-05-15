package customer.action

import com.google.protobuf.empty.Empty
import customer.api.Customer
import customer.api.CustomerService
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerActionImpl(creationContext: ActionCreationContext) extends AbstractCustomerAction {

  private val logger = LoggerFactory.getLogger(classOf[CustomerActionImpl])

  private val customerRegistry = creationContext.getGrpcClient(classOf[CustomerService], "customer-registry")
  override def create(customer: Customer): Action.Effect[Empty] = {
    logger.info(s"Creating customer on customer-registry service $customer");
    effects.asyncReply(customerRegistry.create(customer));
  }
}
