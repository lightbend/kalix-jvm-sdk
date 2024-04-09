/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import kalix.codegen.Log

case class DebugPrintlnLog(debugEnabled: Boolean) extends Log {
  override def debug(message: String): Unit =
    if (debugEnabled) println(s"[DEBUG] $message")
  override def warn(message: String): Unit =
    if (debugEnabled) println(s"[WARNING] $message")
  override def info(message: String): Unit =
    if (debugEnabled) println(s"[INFO] $message")
}
