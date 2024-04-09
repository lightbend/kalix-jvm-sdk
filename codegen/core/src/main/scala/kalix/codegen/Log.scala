/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen

trait Log {
  // On sbt we don't have a neat way to hook into logging,
  // so there debug/info messages are either silent or println'ed, and
  // all other problems should be fatal.
  def debug(message: String): Unit
  def warn(message: String): Unit
  def info(message: String): Unit
}
