/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
