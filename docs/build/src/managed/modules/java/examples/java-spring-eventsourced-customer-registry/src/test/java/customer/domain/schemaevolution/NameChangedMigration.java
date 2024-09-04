package customer.domain.schemaevolution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import kalix.javasdk.JsonMigration;


// tag::name-changed-migration[]
public class NameChangedMigration extends JsonMigration { // <1>

  @Override
  public int currentVersion() {
    return 1; // <2>
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    if (fromVersion < 1) { // <3>
      ObjectNode objectNode = ((ObjectNode) json);
      objectNode.set("reason", TextNode.valueOf("default reason")); // <4>
    }
    return json; // <5>
  }
}
// end::name-changed-migration[]
