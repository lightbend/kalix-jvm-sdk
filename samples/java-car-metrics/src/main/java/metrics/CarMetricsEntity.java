/*
 * Copyright 2021 Lightbend Inc.
 */

package car.metrics;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;

import car.metrics.api.CarMetricsApi;
import car.metrics.domain.CarMetricsDomain;

import com.google.protobuf.Empty;

@EventSourcedEntity(entityType = "eventsourced-car-metrics")
public class CarMetricsEntity {

  private final String entityId;
  private int currentCharge = 0;
  private int fuelCapacity = 0;

  public CarMetricsEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  @CommandHandler
  public Empty recordChargeLevel(CarMetricsApi.ChargeLevel chargeLevel, CommandContext context) {
    CarMetricsDomain.ChargeLevelRecorded fl =
        CarMetricsDomain.ChargeLevelRecorded.newBuilder()
            .setRemainingWatts(chargeLevel.getMetrics().getRemainingWatts())
            .setWattsCapacity(chargeLevel.getMetrics().getWattsCapacity())
            .build();
    context.emit(fl);
    return Empty.getDefaultInstance();
  }

  @EventHandler
  public void chargeLevelRecorded(CarMetricsDomain.ChargeLevelRecorded flr) {
    this.currentCharge = flr.getRemainingWatts();
    this.fuelCapacity = flr.getWattsCapacity();
  }

  @CommandHandler
  public CarMetricsApi.CarMetrics getCarMetrics() {
    CarMetricsApi.CarMetrics metrics =
        CarMetricsApi.CarMetrics.newBuilder()
            .setRemainingWatts(currentCharge)
            .setWattsCapacity(fuelCapacity)
            .build();
    return metrics;
  }
}
