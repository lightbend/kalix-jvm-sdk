package customer.domain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import kalix.javasdk.JsonSupport;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static customer.domain.schemaevolution.CustomerEvent.AddressChanged;
import static customer.domain.schemaevolution.CustomerEvent.CustomerCreated;
import static customer.domain.schemaevolution.CustomerEvent.NameChanged;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerEventSerializationTest {

  @Test
  public void shouldDeserializeWithMandatoryField() {
    //given
    Any serialized = JsonSupport.encodeJson(new CustomerEvent.NameChanged("andre"));

    //when
    NameChanged deserialized = JsonSupport.decodeJson(NameChanged.class, serialized);

    //then
    assertEquals("andre", deserialized.newName());
    assertEquals(Optional.empty(), deserialized.oldName());
    assertEquals("default reason", deserialized.reason());
  }

  @Test
  public void shouldDeserializeWithChangedFieldName() {
    //given
    Address address = new Address("Wall Street", "New York");
    Any serialized = JsonSupport.encodeJson(new CustomerEvent.AddressChanged(address));

    //when
    AddressChanged deserialized = JsonSupport.decodeJson(AddressChanged.class, serialized);

    //then
    assertEquals(address, deserialized.newAddress());
  }

  @Test
  public void shouldDeserializeWithStructureMigration() {
    //given
    Any serialized = JsonSupport.encodeJson(new CustomerCreated("bob@lightbend.com", "bob", "Wall Street", "New York"));

    //when
    CustomerEvent.CustomerCreated deserialized = JsonSupport.decodeJson(CustomerEvent.CustomerCreated.class, serialized);

    //then
    assertEquals("Wall Street", deserialized.address().street());
    assertEquals("New York", deserialized.address().city());
  }

  // tag::testing-deserialization[]
  @Test
  public void shouldDeserializeCustomerCreated_V0() throws InvalidProtocolBufferException {
    // end::testing-deserialization[]
    Any serialized = JsonSupport.encodeJson(new CustomerCreated("bob@lightbend.com", "bob", "Wall Street", "New York"));

    new String(Base64.getEncoder().encode(serialized.toByteArray()));

    // tag::testing-deserialization[]
    String encodedBytes = "Cktqc29uLmthbGl4LmlvL2N1c3RvbWVyLmRvbWFpbi5zY2hlbWFldm9sdXRpb24uQ3VzdG9tZXJFdmVudCRDdXN0b21lckNyZWF0ZWQSVQpTeyJlbWFpbCI6ImJvYkBsaWdodGJlbmQuY29tIiwibmFtZSI6ImJvYiIsInN0cmVldCI6IldhbGwgU3RyZWV0IiwiY2l0eSI6Ik5ldyBZb3JrIn0=";
    byte[] bytes = Base64.getDecoder().decode(encodedBytes.getBytes()); // <1>
    Any serializedAny = Any.parseFrom(ByteString.copyFrom(bytes)); // <2>

    CustomerEvent.CustomerCreated deserialized = JsonSupport.decodeJson(CustomerEvent.CustomerCreated.class,
        serializedAny); // <3>

    assertEquals("Wall Street", deserialized.address().street());
    assertEquals("New York", deserialized.address().city());
  }
  // end::testing-deserialization[]

}