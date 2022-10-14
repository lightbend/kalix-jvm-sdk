/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.springsdk.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Acl {

  Matcher[] allow() default {};

  Matcher[] deny() default {};

  int denyCode() default 0;

  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Matcher {
    String service() default "";

    Principal principal() default Principal.UNSPECIFIED;
  }

  enum Principal {
    UNSPECIFIED,
    /**
     * All (or no) principals. This matches all requests regardless of what principals are
     * associated with it.
     */
    ALL,
    /**
     * The internet. This will match all requests that originated from the internet, and passed
     * through the Kalix ingress via a configured route.
     */
    INTERNET
  }
}
