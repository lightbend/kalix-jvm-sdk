package customer.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kalix.javasdk.JsonMigration;

// tag::customer-created-migration[]
public class CustomerCreatedMigration extends JsonMigration {

  @Override
  public int currentVersion() {
    return 1;
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    if (fromVersion == 0) {
      ObjectNode root = ((ObjectNode) json);
      ObjectNode address = root.with("address"); // <1>
      address.set("street", root.get("street"));
      address.set("city", root.get("city"));
      root.remove("city");
      root.remove("street");
    }
    return json;
  }
}
// end::customer-created-migration[]
