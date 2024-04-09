/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JWT {

  enum JwtMethodMode {
    /**
     * No validation.
     */
    UNSPECIFIED,

    /**
     * Validates that the bearer token is present on the request, in the 'Authorization' header.
     */
    BEARER_TOKEN,

  }

  JwtMethodMode[] validate() default JwtMethodMode.UNSPECIFIED;

  /**
   * If set, then the token extracted from the bearer token must have this issuer.
   *
   * This can be used in combination with the issuer field of configuration for JWT secrets, if
   * there is at least one secret that has this issuer set, then only those secrets with that issuer
   * set will be used for validating or signing this token, so you can be sure that the token did
   * come from a particular issuer.`
   *
   */
  String[] bearerTokenIssuer() default {};


  /**
   * A static claim is a claim that is required to be present on the token, and have a particular
   * value. This can be used to ensure that the token has a particular role, for example.
   * <p>
   * If the claim is not present, or does not have the expected value, then the request will be
   * rejected with a 403 Forbidden response.
   * <p>
   * If the claim is present, but does not have the expected value, then the request will be
   * rejected with a 403 Forbidden response.
   * <p>
   * If the claim is present, and has the expected value, then the request will be allowed to
   * proceed.
   *
   * Each static claim can be configured either with a 'value' or a 'pattern' that will
   * be matched against the value of the claim, but not both.
   */
  @interface StaticClaim {
    /**
     * The claim name needs to be a hardcoded literal (e.g. "role")
     */
    String claim();

    /**
     * The value can be set as: a hardcoded literal (e.g. "admin"), an ENV variable (e.g "${ENV_VAR}")
     * or a combination of both (e.g. "${ENV_VAR}-admin").
     * When declaring multiple values, ALL of those will be required when validating the claim.
     */
    String[] value() default {};

    /**
     *  This receives a regex expression (Java flavor) used to match on the incoming claim value.
     *  Cannot be used in conjunction with `value` field above. It's one or the other.
     *  <p>NOTE: when signing, a static claim defined with a pattern will not be included in the token.
     *
     *  <p>Usage examples:
     *  <ul>
     *  <li>claim value is not empty: "\\S+"</li>
     *  <li>claim value has one of 2 possible values: "^(admin|manager)$"</li>
     *  </ul>
     */
    String pattern() default "";
  }

  /**
   * If set, the static claims provided and their values will be required when calling the service.
   * When multiple claims are provided, all of them will be required to successfully call the service.
   */
  StaticClaim[] staticClaims() default {};
}
