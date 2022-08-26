package wiring.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

  public final String name;
  public final String email;

  @JsonCreator
  public User(@JsonProperty("name") String name,
              @JsonProperty("email") String email) {
    this.name = name;
    this.email = email;
  }
}

