/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
