package wiring.eventsourcedentity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueIncreased {

    public final int value;

    @JsonCreator
    public ValueIncreased(@JsonProperty int value) {
        this.value = value;
    }
}
