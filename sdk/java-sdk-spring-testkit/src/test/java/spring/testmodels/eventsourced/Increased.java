/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testmodels.eventsourced;

public class Increased {

  public final Integer value;

  public Increased(Integer value) {
    this.value = value;
  }
}
