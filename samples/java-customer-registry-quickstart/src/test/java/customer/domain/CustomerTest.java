package customer.domain;

import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class CustomerTest {

  @Test
  @Ignore("to be implemented")
  public void exampleTest() {
    CustomerTestKit service = CustomerTestKit.of(Customer::new);
    // // use the testkit to execute a command
    // // of events emitted, or a final updated state:
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ValueEntityResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
    // // verify the final state after the command
    // assertEquals(expectedState, service.getState());
  }

  @Test
  @Ignore("to be implemented")
  public void createTest() {
    CustomerTestKit service = CustomerTestKit.of(Customer::new);
    // Customer command = Customer.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.create(command);
  }


  @Test
  @Ignore("to be implemented")
  public void getCustomerTest() {
    CustomerTestKit service = CustomerTestKit.of(Customer::new);
    // GetCustomerRequest command = GetCustomerRequest.newBuilder()...build();
    // ValueEntityResult<Customer> result = service.getCustomer(command);
  }


  @Test
  @Ignore("to be implemented")
  public void changeNameTest() {
    CustomerTestKit service = CustomerTestKit.of(Customer::new);
    // ChangeNameRequest command = ChangeNameRequest.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.changeName(command);
  }


  @Test
  @Ignore("to be implemented")
  public void changeAddressTest() {
    CustomerTestKit service = CustomerTestKit.of(Customer::new);
    // ChangeAddressRequest command = ChangeAddressRequest.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.changeAddress(command);
  }


  @Test
  @Ignore("to be implemented")
  public void deleteTest() {
    CustomerTestKit service = CustomerTestKit.of(Customer::new);
    // DeleteRequest command = DeleteRequest.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.delete(command);
  }

}
