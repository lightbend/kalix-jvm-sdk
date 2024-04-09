/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels;

import java.util.List;

public class NestedMessage {
  public String string;
  public SimpleMessage simpleMessage;

  public InstantWrapper instantWrapper;
  public List<InstantEntryForList> instantsList;
  public InstantEntryForArray[] instantArrays;
}
