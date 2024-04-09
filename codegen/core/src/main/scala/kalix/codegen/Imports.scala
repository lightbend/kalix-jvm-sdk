/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
