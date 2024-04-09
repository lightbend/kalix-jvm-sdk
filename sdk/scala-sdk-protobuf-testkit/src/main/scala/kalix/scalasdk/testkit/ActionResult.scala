/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit

import io.grpc.Status

import scala.concurrent.Future

/**
 * Represents the result of an Action handling a command when run in through the testkit.
 *
 * <p>Not for user extension, returned by the generated testkit.
 *
 * @param T
 *   The type of reply that is expected from invoking a command handler
 */
trait ActionResult[T] {

  /** @return true if the call had an effect with a reply, false if not */
  def isReply: Boolean

  /**
   * @return
   *   The reply message if the returned effect was a reply or throws if the returned effect was not a reply.
   */
  def reply: T

  /** @return true if the call was forwarded, false if not */
  def isForward: Boolean

  /**
   * @return
   *   An object with details about the forward. If the result was not a forward an exception is thrown.
   */
  def forwardedTo: DeferredCallDetails[_, T]

  /** @return true if the call was async, false if not */
  def isAsync: Boolean

  /**
   * @return
   *   The future result if the returned effect was an async effect or throws if the returned effect was not async.
   */
  def asyncResult: Future[ActionResult[T]]

  /** @return true if the returned effect was ignore, false if not */
  def isIgnore: Boolean

  /** @return true if the call was an error, false if not */
  def isError: Boolean

  /**
   * @return
   *   The error description returned or throws if the effect returned by the action was not an error
   */
  def errorDescription: String

  /**
   * @return
   *   The error status code returned or throws if the effect returned by the action was not an error
   */
  def errorStatusCode: Status.Code

  /** @return The list of side effects */
  def sideEffects: Seq[DeferredCallDetails[_, _]];
}
