/*
 * Copyright 2021 Lightbend Inc.
 */

package car.metrics;

import com.akkaserverless.javasdk.view.View;
import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.UpdateHandlerContext;
import car.metrics.view.CarMetricsViewModel.CarMetricsState;
import car.metrics.domain.CarMetricsDomain.ChargeLevelRecorded;

import java.util.Optional;

@View
public class CarMetricsView {

  @UpdateHandler
  public CarMetricsState chargeLevelRecorded(
      ChargeLevelRecorded event, Optional<CarMetricsState> state) {
    if (state.isPresent()) {
      return state
          .get()
          .toBuilder()
          .setRemainingWatts(event.getRemainingWatts())
          .setWattsCapacity(event.getWattsCapacity())
          .build();
    } else {
      return CarMetricsState.newBuilder()
          .setRemainingWatts(event.getRemainingWatts())
          .setWattsCapacity(event.getWattsCapacity())
          .build();
    }
  }
}
