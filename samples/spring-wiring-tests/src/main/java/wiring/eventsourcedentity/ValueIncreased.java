package wiring.eventsourcedentity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kalix.springsdk.eventsourced.EntityEvent;

public class ValueIncreased implements EntityEvent {

    public final int value;

    @JsonCreator
    public ValueIncreased(@JsonProperty("value") int value) {
        this.value = value;
    }
}
