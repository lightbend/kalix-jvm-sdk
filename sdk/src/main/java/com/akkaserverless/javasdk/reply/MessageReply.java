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

package com.akkaserverless.javasdk.reply;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.SideEffect;
import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.Reply;

import java.util.Collection;

/** A message reply. */
public interface MessageReply<T> extends Reply<T>, Effect<T> {

  /**
   * The payload of the message reply.
   *
   * @return The payload.
   */
  T payload();

  /**
   * The metadata associated with the message.
   *
   * @return The metadata.
   */
  Metadata metadata();

  MessageReply<T> addSideEffects(Collection<SideEffect> effects);

  MessageReply<T> addSideEffects(SideEffect... effects);
}
