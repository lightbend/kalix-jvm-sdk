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

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JWT {

  enum JwtMethodMode {
    // No validation.
    UNSPECIFIED,
    // Validate the bearer token.
    BEARER_TOKEN,
    // Validate/sign a token field in the message against the message fields.
    //
    // If present, the message must have a token annotated field or the message itself must have
    // validate_bearer_token
    // set to true.
    MESSAGE
  }

  JwtMethodMode[] validate();

  JwtMethodMode[] sign();
  // If set, then the token extracted from the bearer token must have this issuer.
  //
  // This can be used in combination with the issuer field of configuration for JWT secrets, if
  // there is at least one
  // secret that has this issuer set, then only those secrets with that issuer set will be used
  // for validating or
  // signing this token, so you can be sure that the token did come from a particular issuer.
  String[] bearerTokenIssuer();
}
