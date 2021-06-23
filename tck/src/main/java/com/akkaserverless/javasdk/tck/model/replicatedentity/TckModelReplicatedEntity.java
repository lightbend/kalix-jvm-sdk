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
import com.akkaserverless.javasdk.SideEffect;
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
      case "GCounter":
        return context.state(GCounter.class);
      case "PNCounter":
        return context.state(PNCounter.class);
      case "GSet":
        return context.state(GSet.class);
      case "ORSet":
        return context.state(ORSet.class);
      case "LWWRegister":
        return context.state(LWWRegister.class);
      case "Flag":
        return context.state(Flag.class);
      case "ORMap":
        return context.state(ORMap.class);
      case "Vote":
        return context.state(Vote.class);
      default:
        throw new IllegalArgumentException("Unknown replicated data type: " + dataType);
    }
  }

  private static ReplicatedData createReplicatedData(String name, ReplicatedDataFactory factory) {
    String dataType = replicatedDataType(name);
    switch (dataType) {
      case "GCounter":
        return factory.newGCounter();
      case "PNCounter":
        return factory.newPNCounter();
      case "GSet":
        return factory.<String>newGSet();
      case "ORSet":
        return factory.<String>newORSet();
      case "LWWRegister":
        return factory.newLWWRegister("");
      case "Flag":
        return factory.newFlag();
      case "ORMap":
        return factory.<String, ReplicatedData>newORMap();
      case "Vote":
        return factory.newVote();
      default:
        throw new IllegalArgumentException("Unknown replicated data type: " + dataType);
    }
  }

  @CommandHandler
  public Reply<Response> process(Request request, CommandContext context) {
    Reply<Response> reply = null;
    List<SideEffect> e = new ArrayList<>();
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
          e.add(SideEffect.of(serviceTwoRequest(effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          reply = Reply.failure(action.getFail().getMessage());
          break;
      }
    }
    if (reply == null) {
      reply = Reply.message(responseValue());
    }
    return reply.addSideEffects(e);
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
      case GCOUNTER:
        ((GCounter) replicatedData).increment(update.getGcounter().getIncrement());
        break;
      case PNCOUNTER:
        ((PNCounter) replicatedData).increment(update.getPncounter().getChange());
        break;
      case GSET:
        @SuppressWarnings("unchecked")
        GSet<String> gset = (GSet<String>) replicatedData;
        gset.add(update.getGset().getAdd());
        break;
      case ORSET:
        @SuppressWarnings("unchecked")
        ORSet<String> orset = (ORSet<String>) replicatedData;
        switch (update.getOrset().getActionCase()) {
          case ADD:
            orset.add(update.getOrset().getAdd());
            break;
          case REMOVE:
            orset.remove(update.getOrset().getRemove());
            break;
          case CLEAR:
            if (update.getOrset().getClear()) orset.clear();
            break;
        }
        break;
      case LWWREGISTER:
        @SuppressWarnings("unchecked")
        LWWRegister<String> lwwRegister = (LWWRegister<String>) replicatedData;
        String newValue = update.getLwwregister().getValue();
        if (update.getLwwregister().hasClock()) {
          LWWRegisterClock clock = update.getLwwregister().getClock();
          switch (clock.getClockType()) {
            case LWW_REGISTER_CLOCK_TYPE_DEFAULT_UNSPECIFIED:
              lwwRegister.set(newValue);
              break;
            case LWW_REGISTER_CLOCK_TYPE_REVERSE:
              lwwRegister.set(newValue, LWWRegister.Clock.REVERSE, 0);
              break;
            case LWW_REGISTER_CLOCK_TYPE_CUSTOM:
              lwwRegister.set(newValue, LWWRegister.Clock.CUSTOM, clock.getCustomClockValue());
              break;
            case LWW_REGISTER_CLOCK_TYPE_CUSTOM_AUTO_INCREMENT:
              lwwRegister.set(
                  newValue, LWWRegister.Clock.CUSTOM_AUTO_INCREMENT, clock.getCustomClockValue());
              break;
          }
        } else {
          lwwRegister.set(newValue);
        }
        break;
      case FLAG:
        ((Flag) replicatedData).enable();
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
    if (replicatedData instanceof GCounter) {
      GCounter gcounter = (GCounter) replicatedData;
      builder.setGcounter(GCounterValue.newBuilder().setValue(gcounter.getValue()));
    } else if (replicatedData instanceof PNCounter) {
      PNCounter pncounter = (PNCounter) replicatedData;
      builder.setPncounter(PNCounterValue.newBuilder().setValue(pncounter.getValue()));
    } else if (replicatedData instanceof GSet) {
      @SuppressWarnings("unchecked")
      GSet<String> gset = (GSet<String>) replicatedData;
      List<String> elements = new ArrayList<>(gset);
      Collections.sort(elements);
      builder.setGset(GSetValue.newBuilder().addAllElements(elements));
    } else if (replicatedData instanceof ORSet) {
      @SuppressWarnings("unchecked")
      ORSet<String> orset = (ORSet<String>) replicatedData;
      List<String> elements = new ArrayList<>(orset);
      Collections.sort(elements);
      builder.setOrset(ORSetValue.newBuilder().addAllElements(elements));
    } else if (replicatedData instanceof LWWRegister) {
      @SuppressWarnings("unchecked")
      LWWRegister<String> lwwRegister = (LWWRegister<String>) replicatedData;
      builder.setLwwregister(LWWRegisterValue.newBuilder().setValue(lwwRegister.get()));
    } else if (replicatedData instanceof Flag) {
      builder.setFlag(FlagValue.newBuilder().setValue(((Flag) replicatedData).isEnabled()));
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
