/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;

import kalix.javasdk.annotations.Migration;

@Migration(TestVEState2Migration.class)
public record TestVEState2(String s, int i, String newValue) {
}
