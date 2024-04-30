/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.echo;

import org.springframework.stereotype.Component;

@Component
public class Parrot {

  public String repeat(String word) {
    return "Parrot says: '" + word + "'";
  }
}
