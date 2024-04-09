/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.replicatedentity;

import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.SideEffect;
import kalix.javasdk.impl.GrpcDeferredCall;
import kalix.javasdk.impl.InternalContext;
import kalix.javasdk.impl.MetadataImpl;
import kalix.replicatedentity.ReplicatedData;
import kalix.javasdk.replicatedentity.*;
import kalix.tck.model.ReplicatedEntity.*;
import com.example.Components;
import com.example.ComponentsImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReplicatedEntityTckModelEntity extends ReplicatedEntity<ReplicatedData> {

  private final String entityId;

  public ReplicatedEntityTckModelEntity(ReplicatedEntityContext context) {
    entityId = context.entityId();
  }

  // FIXME should come from generated Abstract base class
  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  @Override
  public ReplicatedData emptyData(ReplicatedDataFactory factory) {
    return createReplicatedData(entityId, factory);
  }

  private static String replicatedDataType(String name) {
    return name.split("-")[0];
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
      case "ReplicatedMap":
        return factory.<String, ReplicatedData>newReplicatedMap();
      case "ReplicatedCounterMap":
        return factory.<String>newReplicatedCounterMap();
      case "ReplicatedRegisterMap":
        return factory.<String, String>newReplicatedRegisterMap();
      case "ReplicatedMultiMap":
        return factory.<String, String>newReplicatedMultiMap();
      case "Vote":
        return factory.newVote();
      default:
        throw new IllegalArgumentException("Unknown replicated data type: " + dataType);
    }
  }

  public Effect<Response> process(ReplicatedData data, Request request) {
    Effect.OnSuccessBuilder builder = null;
    Effect<Response> result = null;
    List<SideEffect> sideEffects = new ArrayList<>();
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case UPDATE:
          builder = effects().update(data = applyUpdate(data, action.getUpdate()));
          break;
        case DELETE:
          builder = effects().delete();
          break;
        case FORWARD:
          if (builder == null) {
            result = effects().forward(serviceTwoRequest(action.getForward().getId()));
          } else {
            result = builder.thenForward(serviceTwoRequest(action.getForward().getId()));
          }
          break;
        case EFFECT:
          kalix.tck.model.ReplicatedEntity.Effect effect = action.getEffect();
          sideEffects.add(
              SideEffect.of(serviceTwoRequest(effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          result = effects().error(action.getFail().getMessage());
          break;
      }
    }
    if (builder == null && result == null) {
      return effects().reply(responseValue(data)).addSideEffects(sideEffects);
    } else if (result == null) {
      return builder.thenReply(responseValue(data)).addSideEffects(sideEffects);
    } else {
      return result.addSideEffects(sideEffects);
    }
  }

  private ReplicatedData applyUpdate(ReplicatedData replicatedData, Update update) {
    switch (update.getUpdateCase()) {
      case COUNTER:
        return ((ReplicatedCounter) replicatedData).increment(update.getCounter().getChange());
      case REPLICATED_SET:
        @SuppressWarnings("unchecked")
        ReplicatedSet<String> replicatedSet = (ReplicatedSet<String>) replicatedData;
        switch (update.getReplicatedSet().getActionCase()) {
          case ADD:
            return replicatedSet.add(update.getReplicatedSet().getAdd());
          case REMOVE:
            return replicatedSet.remove(update.getReplicatedSet().getRemove());
          case CLEAR:
            return update.getReplicatedSet().getClear() ? replicatedSet.clear() : replicatedSet;
        }
      case REGISTER:
        @SuppressWarnings("unchecked")
        ReplicatedRegister<String> register = (ReplicatedRegister<String>) replicatedData;
        String newValue = update.getRegister().getValue();
        if (update.getRegister().hasClock()) {
          ReplicatedRegisterClock clock = update.getRegister().getClock();
          switch (clock.getClockType()) {
            case REPLICATED_REGISTER_CLOCK_TYPE_DEFAULT_UNSPECIFIED:
              return register.set(newValue);
            case REPLICATED_REGISTER_CLOCK_TYPE_REVERSE:
              return register.set(newValue, ReplicatedRegister.Clock.REVERSE, 0);
            case REPLICATED_REGISTER_CLOCK_TYPE_CUSTOM:
              return register.set(
                  newValue, ReplicatedRegister.Clock.CUSTOM, clock.getCustomClockValue());
            case REPLICATED_REGISTER_CLOCK_TYPE_CUSTOM_AUTO_INCREMENT:
              return register.set(
                  newValue,
                  ReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT,
                  clock.getCustomClockValue());
          }
        } else {
          return register.set(newValue);
        }
      case REPLICATED_MAP:
        @SuppressWarnings("unchecked")
        ReplicatedMap<String, ReplicatedData> replicatedMap =
            (ReplicatedMap<String, ReplicatedData>) replicatedData;
        switch (update.getReplicatedMap().getActionCase()) {
          case ADD:
            String addKey = update.getReplicatedMap().getAdd();
            return replicatedMap.update(
                addKey,
                replicatedMap.getOrElse(addKey, factory -> createReplicatedData(addKey, factory)));
          case UPDATE:
            String updateKey = update.getReplicatedMap().getUpdate().getKey();
            Update entryUpdate = update.getReplicatedMap().getUpdate().getUpdate();
            ReplicatedData dataValue =
                replicatedMap.getOrElse(
                    updateKey, factory -> createReplicatedData(updateKey, factory));
            return replicatedMap.update(updateKey, applyUpdate(dataValue, entryUpdate));
          case REMOVE:
            String removeKey = update.getReplicatedMap().getRemove();
            return replicatedMap.remove(removeKey);
          case CLEAR:
            return replicatedMap.clear();
        }
      case REPLICATED_COUNTER_MAP:
        @SuppressWarnings("unchecked")
        ReplicatedCounterMap<String> counterMap = (ReplicatedCounterMap<String>) replicatedData;
        switch (update.getReplicatedCounterMap().getActionCase()) {
          case ADD:
            String addKey = update.getReplicatedCounterMap().getAdd();
            return counterMap.increment(addKey, 0);
          case UPDATE:
            String updateKey = update.getReplicatedCounterMap().getUpdate().getKey();
            long change = update.getReplicatedCounterMap().getUpdate().getChange();
            return counterMap.increment(updateKey, change);
          case REMOVE:
            String removeKey = update.getReplicatedCounterMap().getRemove();
            return counterMap.remove(removeKey);
          case CLEAR:
            return counterMap.clear();
        }
      case REPLICATED_REGISTER_MAP:
        @SuppressWarnings("unchecked")
        ReplicatedRegisterMap<String, String> registerMap =
            (ReplicatedRegisterMap<String, String>) replicatedData;
        switch (update.getReplicatedRegisterMap().getActionCase()) {
          case ADD:
            String addKey = update.getReplicatedRegisterMap().getAdd();
            return registerMap.setValue(addKey, "");
          case UPDATE:
            ReplicatedRegisterMapEntryUpdate entryUpdate =
                update.getReplicatedRegisterMap().getUpdate();
            String updateKey = entryUpdate.getKey();
            String updateValue = entryUpdate.getValue();
            if (entryUpdate.hasClock()) {
              ReplicatedRegisterClock clock = entryUpdate.getClock();
              switch (clock.getClockType()) {
                case REPLICATED_REGISTER_CLOCK_TYPE_DEFAULT_UNSPECIFIED:
                  return registerMap.setValue(updateKey, updateValue);
                case REPLICATED_REGISTER_CLOCK_TYPE_REVERSE:
                  return registerMap.setValue(
                      updateKey, updateValue, ReplicatedRegister.Clock.REVERSE, 0);
                case REPLICATED_REGISTER_CLOCK_TYPE_CUSTOM:
                  return registerMap.setValue(
                      updateKey,
                      updateValue,
                      ReplicatedRegister.Clock.CUSTOM,
                      clock.getCustomClockValue());
                case REPLICATED_REGISTER_CLOCK_TYPE_CUSTOM_AUTO_INCREMENT:
                  return registerMap.setValue(
                      updateKey,
                      updateValue,
                      ReplicatedRegister.Clock.CUSTOM_AUTO_INCREMENT,
                      clock.getCustomClockValue());
              }
            } else {
              return registerMap.setValue(updateKey, updateValue);
            }
          case REMOVE:
            String removeKey = update.getReplicatedRegisterMap().getRemove();
            return registerMap.remove(removeKey);
          case CLEAR:
            return registerMap.clear();
        }
      case REPLICATED_MULTI_MAP:
        @SuppressWarnings("unchecked")
        ReplicatedMultiMap<String, String> multiMap =
            (ReplicatedMultiMap<String, String>) replicatedData;
        switch (update.getReplicatedMultiMap().getActionCase()) {
          case UPDATE:
            String updateKey = update.getReplicatedMultiMap().getUpdate().getKey();
            ReplicatedSetUpdate updateValue =
                update.getReplicatedMultiMap().getUpdate().getUpdate();
            switch (updateValue.getActionCase()) {
              case ADD:
                return multiMap.put(updateKey, updateValue.getAdd());
              case REMOVE:
                return multiMap.remove(updateKey, updateValue.getRemove());
              case CLEAR:
                return updateValue.getClear() ? multiMap.removeAll(updateKey) : multiMap;
            }
          case REMOVE:
            String removeKey = update.getReplicatedMultiMap().getRemove();
            return multiMap.removeAll(removeKey);
          case CLEAR:
            return multiMap.clear();
        }
      case VOTE:
        return ((ReplicatedVote) replicatedData).vote(update.getVote().getSelfVote());
      default:
        return replicatedData;
    }
  }

  private Response responseValue(ReplicatedData data) {
    return Response.newBuilder().setState(dataState(data)).build();
  }

  private State dataState(ReplicatedData replicatedData) {
    State.Builder builder = State.newBuilder();
    if (replicatedData instanceof ReplicatedCounter) {
      ReplicatedCounter counter = (ReplicatedCounter) replicatedData;
      builder.setCounter(ReplicatedCounterValue.newBuilder().setValue(counter.getValue()));
    } else if (replicatedData instanceof ReplicatedSet) {
      @SuppressWarnings("unchecked")
      ReplicatedSet<String> set = (ReplicatedSet<String>) replicatedData;
      List<String> elements = new ArrayList<>(set.elements());
      Collections.sort(elements);
      builder.setReplicatedSet(ReplicatedSetValue.newBuilder().addAllElements(elements));
    } else if (replicatedData instanceof ReplicatedRegister) {
      @SuppressWarnings("unchecked")
      ReplicatedRegister<String> register = (ReplicatedRegister<String>) replicatedData;
      builder.setRegister(ReplicatedRegisterValue.newBuilder().setValue(register.get()));
    } else if (replicatedData instanceof ReplicatedMap) {
      @SuppressWarnings("unchecked")
      ReplicatedMap<String, ReplicatedData> replicatedMap =
          (ReplicatedMap<String, ReplicatedData>) replicatedData;
      List<ReplicatedMapEntryValue> entries = new ArrayList<>();
      for (String key : replicatedMap.keySet()) {
        entries.add(
            ReplicatedMapEntryValue.newBuilder()
                .setKey(key)
                .setValue(dataState(replicatedMap.get(key)))
                .build());
      }
      entries.sort(Comparator.comparing(ReplicatedMapEntryValue::getKey));
      builder.setReplicatedMap(ReplicatedMapValue.newBuilder().addAllEntries(entries));
    } else if (replicatedData instanceof ReplicatedCounterMap) {
      @SuppressWarnings("unchecked")
      ReplicatedCounterMap<String> counterMap = (ReplicatedCounterMap<String>) replicatedData;
      List<ReplicatedCounterMapEntryValue> entries = new ArrayList<>();
      for (String key : counterMap.keySet()) {
        entries.add(
            ReplicatedCounterMapEntryValue.newBuilder()
                .setKey(key)
                .setValue(counterMap.get(key))
                .build());
      }
      entries.sort(Comparator.comparing(ReplicatedCounterMapEntryValue::getKey));
      builder.setReplicatedCounterMap(
          ReplicatedCounterMapValue.newBuilder().addAllEntries(entries));
    } else if (replicatedData instanceof ReplicatedRegisterMap) {
      @SuppressWarnings("unchecked")
      ReplicatedRegisterMap<String, String> registerMap =
          (ReplicatedRegisterMap<String, String>) replicatedData;
      List<ReplicatedRegisterMapEntryValue> entries = new ArrayList<>();
      for (String key : registerMap.keySet()) {
        entries.add(
            ReplicatedRegisterMapEntryValue.newBuilder()
                .setKey(key)
                .setValue(registerMap.getValue(key).orElse(""))
                .build());
      }
      entries.sort(Comparator.comparing(ReplicatedRegisterMapEntryValue::getKey));
      builder.setReplicatedRegisterMap(
          ReplicatedRegisterMapValue.newBuilder().addAllEntries(entries));
    } else if (replicatedData instanceof ReplicatedMultiMap) {
      @SuppressWarnings("unchecked")
      ReplicatedMultiMap<String, String> multiMap =
          (ReplicatedMultiMap<String, String>) replicatedData;
      List<ReplicatedMultiMapEntryValue> entries = new ArrayList<>();
      for (String key : multiMap.keySet()) {
        List<String> values = new ArrayList<>(multiMap.get(key));
        Collections.sort(values);
        entries.add(
            ReplicatedMultiMapEntryValue.newBuilder()
                .setKey(key)
                .setValue(ReplicatedSetValue.newBuilder().addAllElements(values))
                .build());
      }
      entries.sort(Comparator.comparing(ReplicatedMultiMapEntryValue::getKey));
      builder.setReplicatedMultiMap(ReplicatedMultiMapValue.newBuilder().addAllEntries(entries));
    } else if (replicatedData instanceof ReplicatedVote) {
      ReplicatedVote vote = (ReplicatedVote) replicatedData;
      builder.setVote(
          VoteValue.newBuilder()
              .setSelfVote(vote.getSelfVote())
              .setVotesFor(vote.getVotesFor())
              .setTotalVoters(vote.getVoters()));
    }
    return builder.build();
  }

  private DeferredCall<Request, Response> serviceTwoRequest(String id) {
    // FIXME: replace with below code once ReplicatedEntityTwo is included in code gen
    // return components().replicatedEntityTwoAction().call(Request.newBuilder().setId(id).build());

    Request request = Request.newBuilder().setId(id).build();
    return new GrpcDeferredCall<>(
        request,
        MetadataImpl.Empty(),
        "kalix.tck.model.replicatedentity.ReplicatedEntityTwo",
        "Call",
        (Metadata metadata) ->
            ((InternalContext) commandContext())
                .getComponentGrpcClient(kalix.tck.model.ReplicatedEntityTwo.class)
                .call(request));
  }
}
