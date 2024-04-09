/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit;

import io.grpc.Status;

import java.util.List;

/**
 * Represents the result of an EventSourcedEntity handling a command when run in through the
 * testkit.
 *
 * <p>Not for user extension, returned by the testkit.
 *
 * @param <R> The type of reply that is expected from invoking a command handler
 */
public interface EventSourcedResult<R> {

  /** @return true if the call had an effect with a reply, false if not */
  boolean isReply();

  /**
   * The reply object from the handler if there was one. If the call had an effect without any reply
   * an exception is thrown
   */
  R getReply();

  /** @return true if the call was forwarded, false if not */
  boolean isForward();

  /**
   * An object with details about the forward. If the result was not a forward an exception is
   * thrown
   */
  DeferredCallDetails<?, R> getForward();

  /** @return true if the call was an error, false if not */
  boolean isError();

  /** The error description. If the result was not an error an exception is thrown */
  String getError();

  /**
   * @return The error status code or throws if the effect returned by the action was not an error.
   */
  Status.Code getErrorStatusCode();

  /**
   * @return The updated state. If the state was not updated (no events emitted) an exeption is
   *     thrown
   */
  Object getUpdatedState();

  boolean didEmitEvents();

  /** @return All the events that were emitted by handling this command. */
  List<Object> getAllEvents();

  /**
   * Look at the next event and verify that it is of type E or fail if not or if there is no next
   * event. If successful this consumes the event, so that the next call to this method looks at the
   * next event from here.
   *
   * @return The next event if it is of type E, for additional assertions.
   */
  <E> E getNextEventOfType(Class<E> expectedClass);

  /** @return The list of side effects */
  List<DeferredCallDetails<?, ?>> getSideEffects();
}
