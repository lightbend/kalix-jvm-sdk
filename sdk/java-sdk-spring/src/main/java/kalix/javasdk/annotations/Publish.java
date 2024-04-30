/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/** Annotation for aggregating ways of publishing outgoing information. */
public @interface Publish {

  /**
   * Annotation for marking a method as producing information to be published on a PubSub or Kafka
   * topic.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Topic {
    /** Assign the name of the topic to be used for eventing out. */
    String value();
  }


  /**
   * Annotation to configure the component to publish an event stream to other Kalix services.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Stream {
    /**
     * Identifier for the event stream. Must be unique inside the same Kalix service.
     */
    String id();
  }
}
