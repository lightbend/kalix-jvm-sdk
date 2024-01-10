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
