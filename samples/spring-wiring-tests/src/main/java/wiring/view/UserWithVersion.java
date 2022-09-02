package wiring.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserWithVersion {
    public final String name;
    public final String email;
    public final int version;

    @JsonCreator
    public UserWithVersion(@JsonProperty String name, @JsonProperty String email, @JsonProperty int version) {
        this.name = name;
        this.email = email;
        this.version = version;
    }
}
