/*
 * Copyright 2021 Lightbend Inc.
 */

package car;

import com.akkaserverless.javasdk.AkkaServerless;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import car.metrics.api.CarMetricsApi;
import car.metrics.domain.CarMetricsDomain;
import car.metrics.CarMetricsEntity;
import car.metrics.CarMetricsView;
import car.metrics.view.CarMetricsViewModel;
import car.metrics.forward.ToSupplierAction;
import car.metrics.forward.ToSupplierForward;

import car.supplier.ChargeSupplierEntity;
import car.supplier.api.ChargeSupplierApi;
import car.supplier.domain.ChargeSupplierDomain;

public final class Main {

  public static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    LOG.info("Car metrics App started");
    SERVICE.start().toCompletableFuture().get();
  }

  public static final AkkaServerless SERVICE =
      new AkkaServerless()
          .registerEventSourcedEntity(
              CarMetricsEntity.class,
              CarMetricsApi.getDescriptor().findServiceByName("CarMetricsService"),
              CarMetricsDomain.getDescriptor())
          .registerView(
              CarMetricsView.class,
              CarMetricsViewModel.getDescriptor().findServiceByName("CarMetricsViewService"),
              "allCarMetrics",
              CarMetricsDomain.getDescriptor())
          .registerAction(
              ToSupplierAction.class,
              ToSupplierForward.getDescriptor().findServiceByName("ToSupplierService"))
          .registerValueEntity(
              ChargeSupplierEntity.class,
              ChargeSupplierApi.getDescriptor().findServiceByName("ChargeSupplierService"),
              ChargeSupplierDomain.getDescriptor());
}
