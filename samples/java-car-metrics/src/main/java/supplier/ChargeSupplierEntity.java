/*
 * Copyright 2021 Lightbend Inc.
 */

package car.supplier;

import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.EntityId;

import car.supplier.api.ChargeSupplierApi;
import car.supplier.domain.ChargeSupplierDomain;

import com.google.protobuf.Empty;
import java.util.Optional;

@ValueEntity(entityType = "valueentity_charge_supplier")
public class ChargeSupplierEntity {

  private final String entityId;

  public ChargeSupplierEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  @CommandHandler
  public Empty lowBattery(ChargeSupplierApi.ChargeChange cc, CommandContext context) {
    // TODO fix division by zero
    double remainingWattsPercent = (cc.getRemainingWatts() * 100) / cc.getWattsCapacity();

    ChargeSupplierDomain.CustomerBatteryState customerBatteryState =
        ChargeSupplierDomain.CustomerBatteryState.newBuilder()
            .setCarId(entityId)
            .setRemainingWatts(cc.getRemainingWatts())
            .setWattsCapacity(cc.getWattsCapacity())
            .setRemainingWattsPercent(remainingWattsPercent)
            .build();
    context.updateState(customerBatteryState);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public ChargeSupplierApi.CustomerBatteryState getBatteryState(
      CommandContext<ChargeSupplierDomain.CustomerBatteryState> ctx) {
    // TODO fix none: java.util.NoSuchElementException: No value present
    ChargeSupplierDomain.CustomerBatteryState opt = ctx.getState().get();
    ChargeSupplierApi.CustomerBatteryState apiOut =
        ChargeSupplierApi.CustomerBatteryState.newBuilder()
            .setCarId(entityId)
            .setRemainingWatts(opt.getRemainingWatts())
            .setWattsCapacity(opt.getWattsCapacity())
            .setRemainingWattsPercent(opt.getRemainingWattsPercent())
            .build();
    return apiOut;
  }
}
