package customer.view

import com.google.protobuf.timestamp.Timestamp
import customer.api.Created
import customer.api.NameChanged
import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext
import org.slf4j.LoggerFactory

import java.time.Instant

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class AllCustomersViewImpl(context: ViewContext) extends AbstractAllCustomersView {

  private val log = LoggerFactory.getLogger(classOf[AllCustomersViewImpl])

  override def emptyState: Customer = Customer.defaultInstance

  override def processCustomerCreated(state: Customer, created: Created): UpdateEffect[Customer] = {
    log.info("Customer {} created: {}", updateContext().eventSubject, created)
    val now = Some(Timestamp(Instant.now()))
    effects.updateState(
      state.copy(
        customerId = created.customerId,
        email = created.email,
        name = created.customerName,
        updates = state.updates + 1,
        created = now,
        lastUpdate = now))
  }

  override def processCustomerNameChanged(state: Customer, nameChanged: NameChanged): UpdateEffect[Customer] = {
    log.info("Customer {} created: {}", updateContext().eventSubject, nameChanged)
    effects.updateState(state
      .copy(name = nameChanged.customerName, updates = state.updates + 1, lastUpdate = Some(Timestamp(Instant.now()))))
  }

}
