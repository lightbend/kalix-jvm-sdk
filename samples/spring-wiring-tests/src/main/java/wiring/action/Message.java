package wiring.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {

  public final String text;

  @JsonCreator
  public Message(@JsonProperty("text") String text) {
    this.text = text;
  }
}
