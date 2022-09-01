package wiring.eventsourcedentity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kalix.springsdk.eventsourced.EntityEvent;

public class ValueMultiplied implements EntityEvent {
    public final int value;

    @JsonCreator
    public ValueMultiplied(@JsonProperty Integer value) {
        this.value = value;
    }
}
