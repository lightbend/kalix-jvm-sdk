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

package kalix.springsdk
import kalix.javasdk.DeferredCall
import com.google.protobuf.any.Any

/**
 * Utility to send requests to other Kalix components by composing a DeferredCall. The target component endpoint should
 * belong to a service on the same project.
 */
trait KalixClient {

  /**
   * Provides utility to do a GET HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use on a cross-component call:
   * {{{
   *     @GetMapping("/{number}/next")
   *     public Effect<Number> nextNumber(@PathVariable Long number) {
   *       var serviceCall = kalixClient.get("/fibonacci/"+number+"/next", Number.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   the resource path where the target endpoint will be reached at. . Query parameters can be passed in as part of
   *   the URI but should be encoded if containing special characters.
   * @param returnType
   *   the type returned by the target endpoint
   * @tparam R
   *   type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def get[R](uri: String, returnType: Class[R]): DeferredCall[Any, R]

  /**
   * Provides utility to do a POST HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     @PostMapping("/next")
   *     public Effect<Number> nextNumber(@RequestBody Number number) {
   *       var serviceCall = kalixClient.post("/fibonacci/next", number, Number.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   The resource path where the target endpoint will be reached at. Query parameters can be passed in as part of the
   *   URI but should be encoded if containing special characters.
   * @param body
   *   The HTTP body type expected by the target endpoint
   * @param returnType
   *   The type returned by the target endpoint
   * @tparam P
   *   Type used as a body for the request
   * @tparam R
   *   Type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def post[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[Any, R]

  /**
   * Provides utility to do a PUT HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use: TODO fix sample here
   * {{{
   *     @PutMapping("/next")
   *     public Effect<Number> nextNumber(@RequestBody Number number) {
   *       var serviceCall = kalixClient.put("/fibonacci/next", number, Number.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   The resource path where the target endpoint will be reached at. Query parameters can be passed in as part of the
   *   URI but should be encoded if containing special characters.
   * @param body
   *   The HTTP body type expected by the target endpoint
   * @param returnType
   *   The type returned by the target endpoint
   * @tparam P
   *   Type used as a body for the request
   * @tparam R
   *   Type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def put[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[Any, R]
}
