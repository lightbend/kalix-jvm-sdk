/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import scala.jdk.CollectionConverters._

import kalix.javasdk.impl.AnySupport
import kalix.javasdk.replicatedentity.ReplicatedSet
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.protocol.replicated_entity.ReplicatedSetDelta

private[kalix] class ReplicatedSetImpl[E](
    anySupport: AnySupport,
    values: Set[E] = Set.empty[E],
    added: Set[E] = Set.empty[E],
    removed: Set[E] = Set.empty[E],
    cleared: Boolean = false)
    extends ReplicatedSet[E]
    with InternalReplicatedData {

  override type Self = ReplicatedSetImpl[E]
  override val name = "ReplicatedSet"

  override def size: Int = values.size

  override def isEmpty: Boolean = values.isEmpty

  override def elements: java.util.Set[E] = values.asJava

  /** for Scala SDK */
  def elementsSet: Set[E] = values

  override def iterator(): java.util.Iterator[E] = values.iterator.asJava

  override def contains(element: E): Boolean = values.contains(element)

  override def add(element: E): ReplicatedSetImpl[E] =
    if (values.contains(element)) {
      this
    } else {
      if (removed.contains(element)) {
        new ReplicatedSetImpl(anySupport, values + element, added, removed - element, cleared)
      } else {
        new ReplicatedSetImpl(anySupport, values + element, added + element, removed, cleared)
      }
    }

  override def remove(element: E): ReplicatedSetImpl[E] =
    if (!values.contains(element)) {
      this
    } else {
      if (values.size == 1) { // just the to-be-removed element
        clear()
      } else {
        if (added.contains(element)) {
          new ReplicatedSetImpl(anySupport, values - element, added - element, removed, cleared)
        } else {
          new ReplicatedSetImpl(anySupport, values - element, added, removed + element, cleared)
        }
      }
    }

  /** for Scala SDK */
  def forall(predicate: E => Boolean): Boolean =
    values.forall(predicate)

  /** for Scala SDK */
  def containsAll(elements: Iterable[E]): Boolean =
    elements.forall(values.contains)

  override def containsAll(elements: java.util.Collection[E]): Boolean =
    containsAll(elements.asScala)

  /** for Scala SDK */
  def addAll(elements: Iterable[E]): ReplicatedSetImpl[E] =
    elements.foldLeft(this) { case (set, element) => set.add(element) }

  override def addAll(elements: java.util.Collection[E]): ReplicatedSetImpl[E] =
    addAll(elements.asScala)

  /** for Scala SDK */
  def retainAll(elements: Iterable[E]): ReplicatedSetImpl[E] =
    retainAll(elements.asJavaCollection)

  override def retainAll(elements: java.util.Collection[E]): ReplicatedSetImpl[E] =
    values.foldLeft(this) { case (set, element) => if (!elements.contains(element)) set.remove(element) else set }

  /** for Scala SDK */
  def removeAll(elements: Iterable[E]): ReplicatedSetImpl[E] =
    elements.foldLeft(this) { case (set, element) => set.remove(element) }

  override def removeAll(elements: java.util.Collection[E]): ReplicatedSetImpl[E] =
    removeAll(elements.asScala)

  override def clear(): ReplicatedSetImpl[E] =
    new ReplicatedSetImpl[E](anySupport, cleared = true)

  override def hasDelta: Boolean = cleared || added.nonEmpty || removed.nonEmpty

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedSet(
      ReplicatedSetDelta(
        cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        added = added.map(anySupport.encodeScala).toSeq))

  override def resetDelta(): ReplicatedSetImpl[E] =
    if (hasDelta) new ReplicatedSetImpl(anySupport, values) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedSetImpl[E]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedSet(ReplicatedSetDelta(cleared, removed, added, _)) =>
      val updatedValue = {
        (if (cleared) Set.empty[E]
         else values -- removed.map(element => anySupport.decodePossiblyPrimitive(element).asInstanceOf[E])) ++
        added.map(element => anySupport.decodePossiblyPrimitive(element).asInstanceOf[E])
      }
      new ReplicatedSetImpl(anySupport, updatedValue)
  }

  override def toString = s"ReplicatedSet(${values.mkString(",")})"

}
