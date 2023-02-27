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

package kalix.javasdk.workflowentity;

import akka.annotation.ApiMayChange;
import io.grpc.Status;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.impl.workflowentity.WorkflowEntityEffectImpl;
import net.jodah.typetools.TypeResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** @param <S> The type of the state for this entity. */
@ApiMayChange
public abstract class WorkflowEntity<S> {


  private Optional<CommandContext> commandContext = Optional.empty();

  private Optional<S> currentState = Optional.empty();

  private boolean stateHasBeenSet = false;

  /**
   * Returns the initial empty state object. This object will be passed into the
   * command and step handlers, until a new state replaces it.
   *
   * <p>Also known as "zero state" or "neutral state".
   *
   * <p>The default implementation of this method returns <code>null</code>. It can be overridden to
   * return a more sensible initial state.
   */
  public S emptyState() {
    return null;
  }

  /**
   * Additional context and metadata for a command handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   *
   * @throws IllegalStateException if accessed outside a handler method
   */
  protected final kalix.javasdk.workflowentity.CommandContext commandContext() {
    return commandContext.orElseThrow(() -> new IllegalStateException("CommandContext is only available when handling a command."));
  }

  /**
   * INTERNAL API
   */
  public void _internalSetCommandContext(Optional<CommandContext> context) {
    commandContext = context;
  }

  /**
   * INTERNAL API
   */
  public void _internalSetCurrentState(S state) {
    stateHasBeenSet = true;
    currentState = Optional.ofNullable(state);
  }

  /**
   * Returns the state as currently stored by Kalix.
   *
   * <p>Note that modifying the state directly will not update it in storage. To save the state, one
   * must call {{@code effects().updateState()}}.
   *
   * <p>This method can only be called when handling a command. Calling it outside a method (eg: in
   * the constructor) will raise a IllegalStateException exception.
   *
   * @throws IllegalStateException if accessed outside a handler method
   */
  @ApiMayChange
  protected final S currentState() {
    // user may call this method inside a command handler and get a null because it's legal
    // to have emptyState set to null.
    if (stateHasBeenSet) return currentState.orElse(null);
    else throw new IllegalStateException("Current state is only available when handling a command.");
  }

  /**
   * @return A workflow definition in a form of steps and transitions between them.
   */
  @ApiMayChange
  public abstract Workflow<S> definition();

  protected final Effect.Builder<S> effects() {
    return WorkflowEntityEffectImpl.apply();
  }

  /**
   * A return type to allow returning failures or attaching effects to messages.
   *
   * @param <T> The type of the message that must be returned by this call.
   */
  public interface Effect<T> {

    /**
     * Construct the effect that is returned by the command handler or a step transition.
     * <p>
     * The effect describes next processing actions, such as updating state, transition to another step
     * and sending a reply.
     *
     * @param <S> The type of the state for this entity.
     */
    interface Builder<S> {

      @ApiMayChange
      PersistenceEffectBuilder<S> updateState(S newState);

      /**
       * Pause the workflow execution and wait for an external input, e.g. via command handler.
       */
      @ApiMayChange
      TransitionalEffect<Void> pause();

      /**
       * Set the step that should be executed.
       *
       * @param stepName The step name that should be executed.
       * @param input    The input param for the step.
       */
      @ApiMayChange
      <I> TransitionalEffect<Void> transitionTo(String stepName, I input);

      /**
       * Finish the workflow execution.
       */
      @ApiMayChange
      TransitionalEffect<Void> end();

      /**
       * Create a message reply.
       *
       * @param replyMessage The payload of the reply.
       * @param <R>          The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <R> Effect<R> reply(R replyMessage);


      /**
       * Reply after for example <code>updateState</code>.
       *
       * @param message  The payload of the reply.
       * @param metadata The metadata for the message.
       * @param <R>      The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <R> Effect<R> reply(R message, Metadata metadata);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param <R>         The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <R> ErrorEffect<R> error(String description);

      /**
       * Create an error reply.
       *
       * @param description The description of the error.
       * @param statusCode  A custom gRPC status code.
       * @param <R>         The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <R> ErrorEffect<R> error(String description, Status.Code statusCode);
    }

    interface ErrorEffect<T> extends Effect<T> {
    }

    /**
     * A workflow effect type that contains information about the transition to the next step. This could be also a special transition to pause or end the workflow.
     */
    interface TransitionalEffect<T> extends Effect<T> {

      /**
       * Reply after for example <code>updateState</code>.
       *
       * @param message The payload of the reply.
       * @param <R>     The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <R> Effect<R> thenReply(R message);

      /**
       * Reply after for example <code>updateState</code>.
       *
       * @param message  The payload of the reply.
       * @param metadata The metadata for the message.
       * @param <R>      The type of the message that must be returned by this call.
       * @return A message reply.
       */
      <R> Effect<R> thenReply(R message, Metadata metadata);
    }

    interface PersistenceEffectBuilder<T> {

      /**
       * Pause the workflow execution and wait for an external input, e.g. via command handler.
       */
      @ApiMayChange
      TransitionalEffect<Void> pause();

      /**
       * Set the step that should be executed.
       *
       * @param stepName The step name that should be executed.
       * @param input    The input param for the step.
       */
      @ApiMayChange
      <I> TransitionalEffect<Void> transitionTo(String stepName, I input);

      /**
       * Set the step that should be executed.
       *
       * @param stepName The step name that should be executed.
       */
      @ApiMayChange
      TransitionalEffect<Void> transitionTo(String stepName);

      /**
       * Finish the workflow execution.
       */
      @ApiMayChange
      TransitionalEffect<Void> end();
    }


  }

  public static class Workflow<S> {

    final private List<Step> steps = new ArrayList<Step>();
    final private Set<String> uniqueNames = new HashSet<>();
    private Optional<Duration> workflowTimeout = Optional.empty();


    private Workflow() {
    }

    public Optional<Step> findByName(String name) {
      return steps.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public Workflow<S> addStep(Step step) {
      if (uniqueNames.contains(step.name()))
        throw new IllegalArgumentException("Name '" + step.name() + "' is already in use by another step in this workflow");

      this.steps.add(step);
      this.uniqueNames.add(step.name());
      return this;
    }

    public void forEachStep(Consumer<Step> stepConsumer) {
      steps.forEach(stepConsumer);
    }

    /**
     * Define a timeout for the duration of the entire workflow. When the timeout expires, the workflow is finished and no transitions are allowed.
     * @param duration Timeout duration
     */
    public Workflow<S> timeout(Duration duration) {
      this.workflowTimeout = Optional.of(duration);
      return this;
    }

    public Optional<Duration> getWorkflowTimeout() {
      return workflowTimeout;
    }
  }


  public Workflow<S> workflow() {
    return new Workflow<>();
  }


  public interface Step {
    String name();
  }

  public static class CallStep<CallInput, DefCallInput, DefCallOutput> implements Step {

    final private String _name;
    final public Function<CallInput, DeferredCall<DefCallInput, DefCallOutput>> callFunc;
    final public Function<DefCallOutput, Effect.TransitionalEffect<Void>> transitionFunc;
    final public Class<CallInput> callInputClass;
    final public Class<DefCallOutput> transitionInputClass;

    public CallStep(String name,
                    Function<CallInput, DeferredCall<DefCallInput, DefCallOutput>> callFunc,
                    Function<DefCallOutput, Effect.TransitionalEffect<Void>> transitionFunc) {
      _name = name;
      this.transitionFunc = transitionFunc;
      this.callFunc = callFunc;
      Class<?>[] callClasses = TypeResolver.resolveRawArguments(Function.class, callFunc.getClass());
      this.callInputClass = (Class<CallInput>) callClasses[0];
      Class<?>[] transitionClasses = TypeResolver.resolveRawArguments(Function.class, transitionFunc.getClass());
      this.transitionInputClass = (Class<DefCallOutput>) transitionClasses[0];
    }

    @Override
    public String name() {
      return this._name;
    }
  }

  public static class AsyncCallStep<CallInput, CallOutput> implements Step {

    final private String _name;
    final public Function<CallInput, CompletionStage<CallOutput>> callFunc;
    final public Function<CallOutput, Effect.TransitionalEffect<Void>> transitionFunc;
    final public Class<CallInput> callInputClass;
    final public Class<CallOutput> transitionInputClass;

    public AsyncCallStep(String name,
                         Function<CallInput, CompletionStage<CallOutput>> callFunc,
                         Function<CallOutput, Effect.TransitionalEffect<Void>> transitionFunc) {
      _name = name;
      this.callFunc = callFunc;
      Class<?>[] callClasses = TypeResolver.resolveRawArguments(Function.class, callFunc.getClass());
      this.callInputClass = (Class<CallInput>) callClasses[0];
      this.transitionFunc = transitionFunc;
      Class<?>[] transitionClasses = TypeResolver.resolveRawArguments(Function.class, transitionFunc.getClass());
      this.transitionInputClass = (Class<CallOutput>) transitionClasses[0];
    }

    @Override
    public String name() {
      return this._name;
    }
  }

  /**
   * Start a step definition with a given step name.
   *
   * @param name Step name.
   * @return Step builder.
   */
  @ApiMayChange
  public static WorkflowEntity.StepBuilder step(String name) {
    return new WorkflowEntity.StepBuilder(name);
  }

  public static class StepBuilder {

    final private String name;

    public StepBuilder(String name) {
      this.name = name;
    }

    /**
     * Build a step action with a call to an existing Kalix component via {@link DeferredCall}.
     *
     * @param callFactory     Factory method for creating deferred call.
     * @param <Input>         Input for deferred call factory, provided by transition method.
     * @param <DefCallInput>  Input for deferred call.
     * @param <DefCallOutput> Output of deferred call.
     * @return Step builder.
     */
    @ApiMayChange
    public <Input, DefCallInput, DefCallOutput> CallStepBuilder<Input, DefCallInput, DefCallOutput> call(Function<Input, DeferredCall<DefCallInput, DefCallOutput>> callFactory) {
      return new CallStepBuilder<>(name, callFactory);
    }

    /**
     * Build a step action with a call to an existing Kalix component via {@link DeferredCall}.
     *
     * @param callSupplier    Factory method for creating deferred call.
     * @param <DefCallInput>  Input for deferred call.
     * @param <DefCallOutput> Output of deferred call.
     * @return Step builder.
     */
    @ApiMayChange
    public <DefCallInput, DefCallOutput> CallStepBuilder<Void, DefCallInput, DefCallOutput> call(Supplier<DeferredCall<DefCallInput, DefCallOutput>> callSupplier) {
      return new CallStepBuilder<>(name, (Void v) -> callSupplier.get());
    }

    /**
     * Build a step action with an async call.
     *
     * @param callFactory Factory method for creating async call.
     * @param <Input>     Input for async call factory, provided by transition method.
     * @param <Output>    Output of async call.
     * @return Step builder.
     */
    @ApiMayChange
    public <Input, Output> AsyncCallStepBuilder<Input, Output> asyncCall(Function<Input, CompletionStage<Output>> callFactory) {
      return new AsyncCallStepBuilder<>(name, callFactory);
    }


    /**
     * Build a step action with an async call.
     *
     * @param callSupplier Factory method for creating async call.
     * @param <Output>     Output of async call.
     * @return Step builder.
     */
    @ApiMayChange
    public <Output> AsyncCallStepBuilder<Void, Output> asyncCall(Supplier<CompletionStage<Output>> callSupplier) {
      return new AsyncCallStepBuilder<>(name, (Void v) -> callSupplier.get());
    }


    public static class CallStepBuilder<Input, DefCallInput, DefCallOutput> {

      final private String name;

      /* callFactory builds the DeferredCall that will be passed to proxy for execution */
      final private Function<Input, DeferredCall<DefCallInput, DefCallOutput>> callFunc;

      public CallStepBuilder(String name, Function<Input, DeferredCall<DefCallInput, DefCallOutput>> callFunc) {
        this.name = name;
        this.callFunc = callFunc;
      }

      /**
       * Transition to the next step based on the result of the step action.
       *
       * @param transitionFunc Function that transform the action result to a {@link Effect.TransitionalEffect}
       * @return CallStep
       */
      @ApiMayChange
      public CallStep<Input, DefCallInput, DefCallOutput> andThen(Function<DefCallOutput, Effect.TransitionalEffect<Void>> transitionFunc) {
        return new CallStep<>(name, callFunc, transitionFunc);
      }
    }

    public static class AsyncCallStepBuilder<CallInput, CallOutput> {

      final private String name;

      /* callFactory builds the DeferredCall that will be passed to proxy for execution */
      final private Function<CallInput, CompletionStage<CallOutput>> callFunc;

      public AsyncCallStepBuilder(String name, Function<CallInput, CompletionStage<CallOutput>> callFunc) {
        this.name = name;
        this.callFunc = callFunc;
      }

      /**
       * Transition to the next step based on the result of the step action.
       *
       * @param transitionFunc Function that transform the action result to a {@link Effect.TransitionalEffect}
       * @return AsyncCallStep
       */
      @ApiMayChange
      public AsyncCallStep<CallInput, CallOutput> andThen(Function<CallOutput, Effect.TransitionalEffect<Void>> transitionFunc) {
        return new AsyncCallStep<>(name, callFunc, transitionFunc);
      }
    }
  }
}
