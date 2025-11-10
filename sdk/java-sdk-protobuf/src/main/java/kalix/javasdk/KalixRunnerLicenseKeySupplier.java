/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import akka.actor.dungeon.LicenseKeySupplier;
import com.typesafe.config.Config;

import java.util.function.Supplier;

/** INTERNAL API: Written in Java to be package private */
class KalixRunnerLicenseKeySupplier implements LicenseKeySupplier {

  @Override
  public void implementing_this_is_a_violation_of_the_akka_license() {}

  @Override
  public String get(Config config) {
    return "3CecWl2eB9O9eHXVjfZjhSA55GSJTudOl8i8gTiPFDLe8MmzU0iPH8V8Ma8kQNmhpu8hzdTesrFa0931MAKNJevZAJimVbQrzDo9CWJ2wgRHAQA55EZrp0OFH8hurn17aPStA57p49suFWd48w8jIeWhzJ47FZPc9wheI9HBORPDdFch8XvogD3tTEJEeaPupCcmizJ27qz0AGSMtD73BuqmRF8sIHWvNAhoMGN4vTabIGi4r";
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
