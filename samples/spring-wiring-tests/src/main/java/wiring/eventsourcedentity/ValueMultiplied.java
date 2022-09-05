package wiring.eventsourcedentity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueMultiplied {
    public final int value;

    @JsonCreator
    public ValueMultiplied(@JsonProperty int value) {
        this.value = value;
    }
}
