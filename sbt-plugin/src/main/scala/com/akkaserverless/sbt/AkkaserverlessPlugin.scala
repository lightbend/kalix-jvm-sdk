/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.sbt

import sbt._
import sbt.Keys._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB
import com.akkaserverless.codegen.scala.gen

object AkkaserverlessPlugin extends AutoPlugin {
  override def trigger = noTrigger
  override def requires = ProtocPlugin

  override def projectSettings: Seq[sbt.Setting[_]] = Seq(
    PB.targets += gen() -> (Compile / sourceManaged).value / "akkaserverless")
}
