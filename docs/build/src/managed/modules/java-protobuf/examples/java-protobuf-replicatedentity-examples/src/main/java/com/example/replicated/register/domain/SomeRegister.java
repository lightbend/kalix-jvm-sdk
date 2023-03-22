package com.example.replicated.register.domain;

import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedRegister;
import com.example.replicated.register.SomeRegisterApi;
import com.google.protobuf.Empty;

public class SomeRegister extends AbstractSomeRegister {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeRegister(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::emptyValue[]
  @Override
  public SomeRegisterDomain.SomeValue emptyValue() {
    return SomeRegisterDomain.SomeValue.getDefaultInstance();
  }
  // end::emptyValue[]

  // tag::update[]
  @Override
  public Effect<Empty> set(
      ReplicatedRegister<SomeRegisterDomain.SomeValue> register, SomeRegisterApi.SetValue command) {
    SomeRegisterDomain.SomeValue newValue = // <1>
        SomeRegisterDomain.SomeValue.newBuilder().setSomeField(command.getValue()).build();
    return effects()
        .update(register.set(newValue)) // <2>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeRegisterApi.CurrentValue> get(
      ReplicatedRegister<SomeRegisterDomain.SomeValue> register, SomeRegisterApi.GetValue command) {
    SomeRegisterDomain.SomeValue value = register.get(); // <1>
    SomeRegisterApi.CurrentValue currentValue = // <2>
        SomeRegisterApi.CurrentValue.newBuilder().setValue(value.getSomeField()).build();
    return effects().reply(currentValue);
  }
  // end::get[]
}
