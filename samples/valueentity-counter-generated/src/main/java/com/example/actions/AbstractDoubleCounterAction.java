/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */

package com.example.actions;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.action.Action;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import java.util.concurrent.CompletionStage;

/** An action. */
public abstract class AbstractDoubleCounterAction extends Action {

  /** Handler for "Increase". */
  public abstract Effect<Empty> increase(CounterApi.IncreaseValue increaseValue);
}