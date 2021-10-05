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

package com.akkaserverless.scalasdk.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedSet => JavaSdkReplicatedSet }
import com.akkaserverless.replicatedentity.ReplicatedData

import scala.collection.immutable
import scala.jdk.CollectionConverters.{ IterableHasAsJava, SetHasAsScala }

/**
 * A Replicated Set that allows both the addition and removal of elements in a set.
 *
 * A removal can only be done if all of the additions that added the key have been seen by this node. This means that,
 * for example if node 1 adds element A, and node 2 also adds element A, then node 1's addition is replicated to node 3,
 * and node 3 deletes it before node 2's addition is replicated, then the element will still be in the map because node
 * 2's addition had not yet been observed by node 3, and will cause the element to be re-added when node 3 receives it.
 * However, if both * additions had been replicated to node 3, then the element will be removed.
 *
 * Care needs to be taken to ensure that the serialized value of elements in the set is stable. For example, if using
 * protobufs, the serialized value of any maps contained in the protobuf is not stable, and can yield a different set of
 * bytes for the same logically equal element. Hence maps should be avoided. Additionally, some changes in protobuf
 * schemas which are backwards compatible from a protobuf perspective, such as changing from sint32 to int32, do result
 * in different serialized bytes, and so must be avoided.
 *
 * @tparam E
 *   The type of elements.
 */
class ReplicatedSet[E] private[scalasdk] (override val internal: JavaSdkReplicatedSet[E]) extends ReplicatedData {

  /**
   * Get the number of elements in this set (its cardinality).
   *
   * @return
   *   the number of elements in the set
   */
  def size: Int = internal.size()

  /**
   * Check whether this set is empty.
   *
   * @return
   *   `true` if this set contains no elements
   */
  def isEmpty: Boolean = internal.isEmpty

  /**
   * Elements of this set as a regular [[immutable.Set]]
   *
   * @return
   *   elements as [[immutable.Set]]
   */
  def elements: immutable.Set[E] =
    immutable.Set.from(internal.elements().asScala)

  /**
   * Check whether this set contains the given element.
   *
   * @param element
   *   element whose presence in this set is to be tested
   * @return
   *   `true` if this set contains the specified element
   */
  def contains(element: E): Boolean =
    internal.contains(element)

  /**
   * Add an element to this set if it is not already present.
   *
   * @param element
   *   element to be added to this set
   * @return
   *   a new set with the additional element, or this unchanged set
   */
  def add(element: E) =
    new ReplicatedSet(internal.add(element))

  /**
   * Remove an element from this set if it is present.
   *
   * @param element
   *   element to be removed from this set
   * @return
   *   a new set without the removed element, or this unchanged set
   */
  def remove(element: E) =
    new ReplicatedSet(internal.remove(element))

  /**
   * Check whether this set contains all the given elements.
   *
   * @param elements
   *   collection to be checked for containment in this set
   * @return
   *   `true` if this set contains all the given elements
   */
  def containsAll(elements: Iterable[E]): Boolean =
    internal.containsAll(elements.asJavaCollection)

  /**
   * Add elements to this set if they're not already present.
   *
   * <p>Effectively the <i>union</i> of two sets.
   *
   * @param elements
   *   collection containing elements to be added to this set
   * @return
   *   a new set with the additional elements, or this unchanged set
   */
  def addAll(elements: Iterable[E]) =
    new ReplicatedSet(internal.addAll(elements.asJavaCollection))

  /**
   * Retain only the elements that are contained in the given collection.
   *
   * <p>Effectively the <i>intersection</i> of two sets.
   *
   * @param elements
   *   collection containing elements to be retained in this set
   * @return
   *   a new set with the retained elements, or this unchanged set
   */
  def retainAll(elements: Iterable[E]) =
    new ReplicatedSet(internal.retainAll(elements.asJavaCollection))

  /**
   * Remove elements from this set if they're present.
   *
   * <p>Effectively the <i>asymmetric set difference</i> of two sets.
   *
   * @param elements
   *   collection containing elements to be removed from this set
   * @return
   *   a new set without the removed elements, or this unchanged set
   */
  def removeAll(elements: Iterable[E]) =
    new ReplicatedSet(internal.removeAll(elements.asJavaCollection))

  /**
   * Remove all elements from this set.
   *
   * @return
   *   a new empty set
   */
  def clear =
    new ReplicatedSet(internal.clear())
}
