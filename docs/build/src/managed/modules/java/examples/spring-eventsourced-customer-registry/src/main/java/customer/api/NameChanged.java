package customer.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NameChanged implements CustomerEvent {

   final public String newName;

  @JsonCreator
  public NameChanged(@JsonProperty String newName) {
    this.newName = newName;
  }
}
