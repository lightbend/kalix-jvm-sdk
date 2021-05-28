/*
 * Copyright 2021 Lightbend Inc.
 */

package car.metrics.forward;

import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.Reply;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.action.Handler;

import car.metrics.domain.CarMetricsDomain.ChargeLevelRecorded;
import car.supplier.api.ChargeSupplierApi;

import com.google.protobuf.Empty;
import com.google.protobuf.Any;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action
public class ToSupplierAction {

  private final String serviceName = "car.supplier.api.ChargeSupplierService";
  private final ServiceCallRef<ChargeSupplierApi.ChargeChange> lowBatteryRef;

  private final int LOW_BATTERY = 25;

  public ToSupplierAction(ActionCreationContext context) {
    lowBatteryRef =
        context
            .serviceCallFactory()
            .lookup(serviceName, "LowBattery", ChargeSupplierApi.ChargeChange.class);
  }

  @Handler
  public Reply<Empty> forwardChargeChange(ChargeLevelRecorded chargeChange, ActionContext ctx) {
    double percentage = (chargeChange.getRemainingWatts() * 100) / chargeChange.getWattsCapacity();
    ChargeSupplierApi.ChargeChange change =
        ChargeSupplierApi.ChargeChange.newBuilder()
            .setRemainingWatts(chargeChange.getRemainingWatts())
            .setWattsCapacity(chargeChange.getWattsCapacity())
            .setCarId(ctx.eventSubject().get())
            .build();

    if (percentage < LOW_BATTERY) {
      return Reply.forward(lowBatteryRef.createCall(change));
    } else {
      return Reply.noReply();
    }
  }

  @Handler
  public Empty catchOthers(Any in) {
    return Empty.getDefaultInstance();
  }
}
