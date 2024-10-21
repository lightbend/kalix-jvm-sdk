/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import akka.actor.dungeon.LicenseKeySupplier;
import com.typesafe.config.Config;

import java.util.function.Supplier;

/**
 * INTERNAL API: Written in Java to be package private
 */
class KalixRunnerLicenseKeySupplier implements LicenseKeySupplier {

  @Override
  public void implementing_this_is_a_violation_of_the_akka_license() {
  }

  @Override
  public String get(Config config) {
    // FIXME
    return "";
  }

  <A> A aroundActorSystemCreation(Supplier<A> creator) {
    LicenseKeySupplier.instance().set(this);
    try {
      return creator.get();
    } finally {
      LicenseKeySupplier.instance().set(null); // clear ThreadLocal
    }
  }


}
