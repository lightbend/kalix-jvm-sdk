package customer.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class JacksonTest {

  @Test
  public void testCustomerAddressChange() throws JsonProcessingException {
    CustomerEvent.CustomerCreated customerCreated = new CustomerEvent.CustomerCreated("email", "name", new Address("street", "some city"));
    ObjectMapper mapper = new ObjectMapper();
    String valueAsString = mapper.writeValueAsString(customerCreated);
    System.out.println(valueAsString);

    CustomerEvent customerEvent = mapper.readValue(valueAsString, CustomerEvent.class);

    System.out.println(customerEvent);
  }
}
