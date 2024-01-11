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

/**
 * Annotation used in the scope of a view for providing the query that will be used to explore data
 * from that view.
 *
 * <p><b>Note: </b>the actual method implementation is irrelevant as the method itself is never
 * actually executed, only the query
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {
  /**
   * Assigns the actual query which makes use of the enclosing entity table name as source of data
   * for composition.
   */
  String value();

  /**
   * If enabled, initially, the normal query results are returned, but the stream
   * does not complete once the full result has been streamed, instead the stream
   * is kept open and updates and new entries added to the view are streamed.
   *
   * Can only be enabled in stream methods returning Flux.
   */
  boolean streamUpdates() default false;
}
