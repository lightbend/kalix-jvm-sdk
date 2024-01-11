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

package kalix.spring.testmodels;

import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Acl.DenyStatusCode;
import kalix.javasdk.annotations.Acl.Matcher;
import kalix.javasdk.annotations.Acl.Principal;

public class AclTestModels {

  public static class MainWithoutAnnotation {}

  @Acl(allow = @Matcher(service = "*"))
  public static class MainAllowAllServices {}

  @Acl(allow = {@Matcher(service = "foo"), @Matcher(service = "bar")})
  public static class MainAllowListOfServices {}

  @Acl(allow = @Matcher(principal = Principal.INTERNET))
  public static class MainAllowPrincipalInternet {}

  @Acl(allow = @Matcher(principal = Principal.ALL))
  public static class MainAllowPrincipalAll {}

  @Acl(allow = @Matcher(principal = Principal.ALL, service = "*"))
  public static class MainWithInvalidAllowAnnotation {}

  @Acl(deny = @Matcher(service = "*"))
  public static class MainDenyAllServices {}

  @Acl(deny = {@Matcher(service = "foo"), @Matcher(service = "bar")})
  public static class MainDenyListOfServices {}

  @Acl(deny = @Matcher(principal = Principal.INTERNET))
  public static class MainDenyPrincipalInternet {}

  @Acl(deny = @Matcher(principal = Principal.ALL))
  public static class MainDenyPrincipalAll {}

  @Acl(allow = @Matcher(principal = Principal.ALL, service = "*"))
  public static class MainWithInvalidDenyAnnotation {}

  @Acl(denyCode = DenyStatusCode.CONFLICT)
  public static class MainDenyWithCode {}
}
