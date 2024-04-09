/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.valueentity;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Id("timerId")
@TypeId("timer")
@RequestMapping("/timer/{timerId}")
public class TimeTrackerEntity extends ValueEntity<TimeTrackerEntity.TimerState> {



  public static class TimerState {

    final public String name;
    final public Instant createdTime;
    final public List<TimerEntry> entries;

    public TimerState(String name, Instant createdTime, List<TimerEntry> entries) {
      this.name = name;
      this.createdTime = createdTime;
      this.entries = entries;
    }
  }
  public static class TimerEntry {
    final public Instant started;
    final public Instant stopped = Instant.MAX;

    public TimerEntry(Instant started) {
      this.started = started;
    }
  }

  @PostMapping()
  public Effect<String> start(@PathVariable String timerId) {
    if (currentState() == null)
      return effects().updateState(new TimerState(timerId, Instant.now(), new ArrayList<>())).thenReply("Created");
    else
      return effects().error("Already created");
  }
}
