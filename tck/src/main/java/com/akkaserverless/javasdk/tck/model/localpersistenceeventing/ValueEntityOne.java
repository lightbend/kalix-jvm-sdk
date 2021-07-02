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

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.impl.reply.MessageReplyImpl;
import com.akkaserverless.javasdk.reply.MessageReply;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

@ValueEntity(entityType = "valuechangeseventing-one")
public class ValueEntityOne extends ValueEntityBase<Object> {
  @CommandHandler
  public MessageReply<Empty> updateValue(
      LocalPersistenceEventing.UpdateValueRequest value, CommandContext<Object> ctx) {
    if (value.hasValueOne()) {
      return effects().updateState(value.getValueOne()).thenReply(Empty.getDefaultInstance());
    } else {
      return effects().updateState(value.getValueTwo()).thenReply(Empty.getDefaultInstance());
    }
  }

  @Override
  protected Object emptyState() {
    return null;
  }
}
