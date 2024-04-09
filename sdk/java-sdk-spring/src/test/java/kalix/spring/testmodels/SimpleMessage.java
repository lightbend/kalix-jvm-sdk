/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels;

import java.time.Instant;

public class SimpleMessage {
  // all them primitives
  public char c;
  public byte by;
  public int n;
  public long l;
  public double d;
  public float f;
  public boolean bo;
  // boxed primitives
  public Character cO;
  public Byte bO;
  public Integer nO;
  public Long lO;
  public Double dO;
  public Float fO;
  public Boolean boO;
  // common object types mapping to proto primitives
  public String s;
  // arrays
  public int[] iA;
  public String[] sA;
  public Instant inst;
}
