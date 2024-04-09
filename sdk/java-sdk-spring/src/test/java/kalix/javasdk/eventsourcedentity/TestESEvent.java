/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import kalix.javasdk.annotations.Migration;
import kalix.javasdk.annotations.TypeName;

public interface TestESEvent {

  @Migration(Event1Migration.class)
  record Event1(String s) implements TestESEvent {
  }

  @Migration(Event2Migration.class)
  record Event2(int newName) implements TestESEvent {
  }

  @TypeName("old-event-3")
  record Event3(boolean b) implements OldTestESEvent {
  }

  @Migration(Event4Migration.class)
  record Event4(String anotherString) implements OldTestESEvent {
  }
}
