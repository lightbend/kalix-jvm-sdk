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

package com.akkaserverless.javasdk.tck.model.replicatedentity;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.replicatedentity.*;
import com.akkaserverless.tck.model.ReplicatedEntity.*;

import java.util.*;

@ReplicatedEntity
public class TckModelReplicatedEntity {

  private final ReplicatedData replicatedData;

  private final ServiceCallRef<Request> serviceTwo;

  public TckModelReplicatedEntity(ReplicatedEntityCreationContext context) {
    @SuppressWarnings("unchecked")
    Optional<ReplicatedData> initialised =
        (Optional<ReplicatedData>) initialData(context.entityId(), context);
    replicatedData = initialised.orElseGet(() -> createReplicatedData(context.entityId(), context));
    serviceTwo =
        context
            .serviceCallFactory()
            .lookup(
                "akkaserverless.tck.model.replicatedentity.ReplicatedEntityTwo",
                "Call",
                Request.class);
  }

  private static String replicatedDataType(String name) {
    return name.split("-")[0];
  }

  private static Optional<? extends ReplicatedData> initialData(
      String name, ReplicatedEntityContext context) {
    String dataType = replicatedDataType(name);
    switch (dataType) {
      case "ReplicatedCounter":
        return context.state(ReplicatedCounter.class);
      case "ReplicatedSet":
        return context.state(ReplicatedSet.class);
      case "ReplicatedRegister":
        return context.state(ReplicatedRegister.class);
      case "ORMap":
        return context.state(ORMap.class);
      case "ReplicatedCounterMap":
        return context.state(ReplicatedCounterMap.class);
      case "ReplicatedRegisterMap":
        return context.state(ReplicatedRegisterMap.class);
      case "Vote":
        return context.state(Vote.class);
      default:
        throw new IllegalArgumentException("Unknown replicated data type: " + dataType);
    }
  }

  private static ReplicatedData createReplicatedData(String name, ReplicatedDataFactory factory) {
    String dataType = replicatedDataType(name);
    switch (dataType) {
      case "ReplicatedCounter":
        return factory.newCounter();
      case "ReplicatedSet":
        return factory.<String>newReplicatedSet();
      case "ReplicatedRegister":
        return factory.newRegister("");
      case "ORMap":
        return factory.<String, ReplicatedData>newORMap();
      case "ReplicatedCounterMap":
        return factory.<String>newReplicatedCounterMap();
      case "ReplicatedRegisterMap":
        return factory.<String, String>newReplicatedRegisterMap();
      case "Vote":
        return factory.newVote();
      default:
        throw new IllegalArgumentException("Unknown replicated data type: " + dataType);
    }
  }

  @CommandHandler
  public Reply<Response> process(Request request, CommandContext context) {
    Reply<Response> reply = null;
    List<com.akkaserverless.javasdk.Effect> e = new ArrayList<>();
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case UPDATE:
          applyUpdate(replicatedData, action.getUpdate());
          switch (action.getUpdate().getWriteConsistency()) {
            case UPDATE_WRITE_CONSISTENCY_LOCAL_UNSPECIFIED:
              context.setWriteConsistency(WriteConsistency.LOCAL);
              break;
            case UPDATE_WRITE_CONSISTENCY_MAJORITY:
              context.setWriteConsistency(WriteConsistency.MAJORITY);
              break;
            case UPDATE_WRITE_CONSISTENCY_ALL:
              context.setWriteConsistency(WriteConsistency.ALL);
              break;
          }
          break;
        case DELETE:
          context.delete();
          break;
        case FORWARD:
          reply = Reply.forward(serviceTwoRequest(action.getForward().getId()));
          break;
        case EFFECT:
          Effect effect = action.getEffect();
          e.add(
              com.akkaserverless.javasdk.Effect.of(
                  serviceTwoRequest(effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          reply = Reply.failure(action.getFail().getMessage());
          break;
      }
    }
    if (reply == null) {
      reply = Reply.message(responseValue());
    }
    return reply.addEffects(e);
  }

  @CommandHandler
  public Optional<Response> processStreamed(
      StreamedRequest request, StreamedCommandContext<Response> context) {
    if (context.isStreamed()) {
      context.onChange(
          subscription -> {
            for (Effect effect : request.getEffectsList())
              subscription.effect(serviceTwoRequest(effect.getId()), effect.getSynchronous());
            if (request.hasEndState() && dataState(replicatedData).equals(request.getEndState()))
              subscription.endStream();
            return request.getEmpty() ? Optional.empty() : Optional.of(responseValue());
          });
      if (request.hasCancelUpdate())
        context.onCancel(cancelled -> applyUpdate(replicatedData, request.getCancelUpdate()));
    }
    if (request.hasInitialUpdate()) applyUpdate(replicatedData, request.getInitialUpdate());
    return request.getEmpty() ? Optional.empty() : Optional.of(responseValue());
  }

  private void applyUpdate(ReplicatedData replicatedData, Update update) {
    switch (update.getUpdateCase()) {
      case COUNTER:
        ((ReplicatedCounter) replicatedData).increment(update.getCounter().getChange());
        break;
      case REPLICATED_SET:
        @SuppressWarnings("unchecked")
        ReplicatedSet<String> replicatedSet = (ReplicatedSet<String>) replicatedData;
        switch (update.getReplicatedSet().getActionCase()) {
          case ADD:
            replicatedSet.add(update.getReplicatedSet().getAdd());
            break;
          case REMOVE:
            replicatedSet.remove(update.getReplicatedSet().getRemove());
            break;
          case CLEAR:
            if (update.getReplicatedSet().getClear()) replicatedSet.clear();
            break;
        }
        break;
      case REGISTER:
        @SuppressWarnings("unchecked")
        ReplicatedRegister<String> register = (ReplicatedRegister<String>) replicatedData;
        String newValue = update.getRegister().getValue();
        if (update.getRegister().hasClock()) {
          ReplicatedRegisterClock clock = update.getRegister().getClock();
          switch (clock.getClockType()) {
            case REPLICATED_REGISTER_CLOCK_TYPE_DEFAULT_UNSPECIFIED:
              register.set(newValue);
              break;
            case REPLICATED_REGISTER_CLOCK_TYPE_REVERSE:
              register.set(newValue, ReplicatedRegister.Clock.REVERSE, 0);
              break;
            case REPLICATED_REGISTER_CLOCK_TYPE_CUSTOM:
              register.set(newValue, ReplicatedRegister.Clock.CUSTOM, clock.getCustomClockValue());
              break;
            case REPLICATED_REGISTER_CLOCK_TYPE_CUSTOM_AUTO_INCREMENT:
              register.set(
                  newValue,
                  ReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT,
                  clock.getCustomClockValue());
              break;
          }
        } else {
          register.set(newValue);
        }
        break;
      case ORMAP:
        @SuppressWarnings("unchecked")
        ORMap<String, ReplicatedData> ormap = (ORMap<String, ReplicatedData>) replicatedData;
        switch (update.getOrmap().getActionCase()) {
          case ADD:
            String addKey = update.getOrmap().getAdd();
            ormap.getOrCreate(addKey, factory -> createReplicatedData(addKey, factory));
            break;
          case UPDATE:
            String updateKey = update.getOrmap().getUpdate().getKey();
            Update entryUpdate = update.getOrmap().getUpdate().getUpdate();
            ReplicatedData dataValue =
                ormap.getOrCreate(updateKey, factory -> createReplicatedData(updateKey, factory));
            applyUpdate(dataValue, entryUpdate);
            break;
          case REMOVE:
            String removeKey = update.getOrmap().getRemove();
            ormap.remove(removeKey);
            break;
          case CLEAR:
            ormap.clear();
            break;
        }
        break;
      case VOTE:
        ((Vote) replicatedData).vote(update.getVote().getSelfVote());
        break;
    }
  }

  private Response responseValue() {
    return Response.newBuilder().setState(dataState(replicatedData)).build();
  }

  private State dataState(ReplicatedData replicatedData) {
    State.Builder builder = State.newBuilder();
    if (replicatedData instanceof ReplicatedCounter) {
      ReplicatedCounter pncounter = (ReplicatedCounter) replicatedData;
      builder.setCounter(ReplicatedCounterValue.newBuilder().setValue(pncounter.getValue()));
    } else if (replicatedData instanceof ReplicatedSet) {
      @SuppressWarnings("unchecked")
      ReplicatedSet<String> orset = (ReplicatedSet<String>) replicatedData;
      List<String> elements = new ArrayList<>(orset);
      Collections.sort(elements);
      builder.setReplicatedSet(ReplicatedSetValue.newBuilder().addAllElements(elements));
    } else if (replicatedData instanceof ReplicatedRegister) {
      @SuppressWarnings("unchecked")
      ReplicatedRegister<String> register = (ReplicatedRegister<String>) replicatedData;
      builder.setRegister(ReplicatedRegisterValue.newBuilder().setValue(register.get()));
    } else if (replicatedData instanceof ORMap) {
      @SuppressWarnings("unchecked")
      ORMap<String, ReplicatedData> ormap = (ORMap<String, ReplicatedData>) replicatedData;
      List<ORMapEntryValue> entries = new ArrayList<>();
      for (Map.Entry<String, ReplicatedData> entry : ormap.entrySet()) {
        entries.add(
            ORMapEntryValue.newBuilder()
                .setKey(entry.getKey())
                .setValue(dataState(entry.getValue()))
                .build());
      }
      entries.sort(Comparator.comparing(ORMapEntryValue::getKey));
      builder.setOrmap(ORMapValue.newBuilder().addAllEntries(entries));
    } else if (replicatedData instanceof Vote) {
      Vote vote = (Vote) replicatedData;
      builder.setVote(
          VoteValue.newBuilder()
              .setSelfVote(vote.getSelfVote())
              .setVotesFor(vote.getVotesFor())
              .setTotalVoters(vote.getVoters()));
    }
    return builder.build();
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwo.createCall(Request.newBuilder().setId(id).build());
  }
}
