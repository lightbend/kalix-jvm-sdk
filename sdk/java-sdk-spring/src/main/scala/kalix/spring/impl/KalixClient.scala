/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.impl

import com.google.protobuf.any.Any
import kalix.javasdk.DeferredCall

/**
 * INTERNAL API
 *
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
   *     public Effect<Number> nextNumber(Long number) {
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
   *     public Effect<Number> nextNumber(Number number) {
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
   * Provides utility to do a POST HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     public Effect<Confirmation> createCounter() {
   *       var serviceCall = kalixClient.post("/counter/create", Confirmation.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   The resource path where the target endpoint will be reached at. Query parameters can be passed in as part of the
   *   URI but should be encoded if containing special characters.
   * @param returnType
   *   The type returned by the target endpoint
   * @tparam R
   *   Type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def post[R](uri: String, returnType: Class[R]): DeferredCall[Any, R]

  /**
   * Provides utility to do a PUT HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     public Effect<Number> nextNumber(Number number) {
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

  /**
   * Provides utility to do a PUT HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     public Effect<Number> nextNumber(Number number) {
   *       var serviceCall = kalixClient.put("/fibonacci/next", number, Number.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   The resource path where the target endpoint will be reached at. Query parameters can be passed in as part of the
   *   URI but should be encoded if containing special characters.
   * @param returnType
   *   The type returned by the target endpoint
   * @tparam R
   *   Type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def put[R](uri: String, returnType: Class[R]): DeferredCall[Any, R]

  /**
   * Provides utility to do a PATCH HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     public Effect<User> createUser(String email) {
   *       var serviceCall = kalixClient.patch("/user/" + user.id + "/email", email, User.class);
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
  def patch[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[Any, R]

  /**
   * Provides utility to do a PATCH HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     public Effect<User> createUser(String email) {
   *       var serviceCall = kalixClient.patch("/user/" + user.id + "/email", email, User.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   The resource path where the target endpoint will be reached at. Query parameters can be passed in as part of the
   *   URI but should be encoded if containing special characters.
   * @param returnType
   *   The type returned by the target endpoint
   * @tparam R
   *   Type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def patch[R](uri: String, returnType: Class[R]): DeferredCall[Any, R]

  /**
   * Provides utility to do a DELETE HTTP request to a target endpoint belonging to a Kalix component. Such endpoint is
   * identified by a resource path (i.e. excluding authority and scheme).
   *
   * Example of use:
   * {{{
   *     public Effect<String> deleteUser(@RequestBody String userId) {
   *       var serviceCall = kalixClient.delete("/user/"+userId, String.class);
   *       return effects().forward(serviceCall);
   *     }
   * }}}
   *
   * @param uri
   *   The resource path where the target endpoint will be reached at. Query parameters can be passed in as part of the
   *   URI but should be encoded if containing special characters.
   * @param returnType
   *   The type returned by the target endpoint
   * @tparam R
   *   Type returned as response from the target endpoint
   * @return
   *   a [[kalix.javasdk.DeferredCall]] to be used in forwards and timers or to be executed in place
   */
  def delete[R](uri: String, returnType: Class[R]): DeferredCall[Any, R]
}
