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

package kalix.codegen

class Imports(val currentPackage: String, val _imports: Seq[String]) {

  val imports: Seq[String] =
    _imports.distinct
      .filterNot(isInCurrentPackage)
      .filterNot(i => clashingNames.contains(i.split("\\.").last))

  lazy val clashingNames: Set[String] =
    _imports.distinct
      .map(_.split("\\.").last)
      .groupBy(identity)
      .filter(_._2.size > 1)
      .keySet

  def ordered: List[Seq[String]] = {
    val (stdlib, other) =
      imports.partition(i => i.startsWith("java.") || i.startsWith("javax.") || i.startsWith("scala."))
    List(other.sorted, stdlib.sorted).filter(_.nonEmpty)
  }

  private def isInCurrentPackage(imp: String): Boolean = {
    val i = imp.lastIndexOf('.')
    if (i == -1)
      currentPackage == ""
    else
      currentPackage == imp.substring(0, i)
  }

  def contains(imp: String): Boolean = imports.contains(imp)
}
