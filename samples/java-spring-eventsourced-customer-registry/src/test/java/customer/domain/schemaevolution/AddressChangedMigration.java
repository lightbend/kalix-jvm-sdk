package customer.domain.schemaevolution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kalix.javasdk.JsonMigration;

import java.util.List;

// tag::address-changed-migration[]
// tag::name-migration[]
public class AddressChangedMigration extends JsonMigration {

  @Override
  public int currentVersion() {
    return 1;
  }

  // end::address-changed-migration[]

  @Override
  public List<String> supportedClassNames() {
    return List.of("customer.domain.CustomerEvent$CustomerAddressChanged"); // <1>
  }

  // end::name-migration[]
  // tag::address-changed-migration[]
  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    if (fromVersion < 1) {
      ObjectNode objectNode = ((ObjectNode) json);
      JsonNode oldField = json.get("address"); // <1>
      objectNode.set("newAddress", oldField); // <2>
      objectNode.remove("address"); // <3>
    }
    return json;
  }
// tag::name-migration[]
}
// end::name-migration[]
// end::address-changed-migration[]
