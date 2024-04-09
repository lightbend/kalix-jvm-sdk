/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels;

import java.time.Instant;

enum Level {HIGH, LOW};

public record TimeEnum(Instant time, Level lev){}