/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import akka.annotation.ApiMayChange;
import io.grpc.Status;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.StatusCode;
import kalix.javasdk.impl.workflow.WorkflowEffectImpl;
import kalix.javasdk.timer.TimerScheduler;
import kalix.javasdk.workflow.AbstractWorkflow.RecoverStrategy.MaxRetries;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractWorkflow<S> {

  private Optional<CommandContext> commandContext = Optional.empty();
  private Optional<TimerScheduler> timerScheduler = Optional.empty();

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
  protected final CommandContext commandContext() {
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
  public void _internalSetTimerScheduler(Optional<TimerScheduler> timerScheduler) {
    this.timerScheduler = timerScheduler;
  }

  /**
   * Returns a {@link TimerScheduler} that can be used to schedule further in time.
   */
  public final TimerScheduler timers() {
    return timerScheduler.orElseThrow(() -> new IllegalStateException("Timers can only be scheduled or cancelled when handling a command or running a step action."));
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
  public abstract WorkflowDef<S> definition();

  protected final Effect.Builder<S> effects() {
    return WorkflowEffectImpl.apply();
  }

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled.
   * You can think of it as a set of instructions you are passing to Kalix. Kalix will process the instructions on your
   * behalf and ensure that any data that needs to be persisted will be persisted.
   * <p>
   * Each Kalix component defines its own effects, which are a set of predefined
   * operations that match the capabilities of that component.
   * <p>
   * A Workflow Effect can either:
   * <p>
   * <ul>
   *   <li>update the state of the workflow
   *   <li>define the next step to be executed (transition)
   *   <li>pause the workflow
   *   <li>end the workflow
   *   <li>fail the step or reject a command by returning an error
   *   <li>reply to incoming commands
   * </ul>
   * <p>
   * <p>
   *  @param <T> The type of the message that must be returned by this call.
   */
  public interface Effect<T> {

    /**
     * Construct the effect that is returned by the command handler or a step transition.
     * <p>
     * The effect describes next processing actions, such as updating state, transition to another step
     * and sending a reply.
     *
     * @param <S> The type of the state for this workflow.
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
       * Defines the next step to which the workflow should transition to.
       * <p>
       * The step definition identified by {@code stepName} must have an input parameter of type I.
       * In other words, the next step call (or asyncCall) must have been defined with a {@link Function} that
       * accepts an input parameter of type I.
       *
       * @param stepName The step name that should be executed next.
       * @param input    The input param for the next step.
       */
      @ApiMayChange
      <I> TransitionalEffect<Void> transitionTo(String stepName, I input);

      /**
       * Defines the next step to which the workflow should transition to.
       * <p>
       * The step definition identified by {@code stepName} must not have an input parameter.
       * In other words, the next step call (or asyncCall) must have been defined with a {@link Supplier} function.
       *
       * @param stepName The step name that should be executed next.
       */
      @ApiMayChange
      TransitionalEffect<Void> transitionTo(String stepName);


      /**
       * Finish the workflow execution.
       * After transition to {@code end}, no more transitions are allowed.
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
       * Create an error reply with a gRPC status code.
       *
       * @param description The description of the error.
       * @param statusCode  A custom gRPC status code.
       * @param <R>         The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <R> ErrorEffect<R> error(String description, Status.Code statusCode);

      /**
       * Create an error reply with an HTTP status code.
       *
       * @param description   The description of the error.
       * @param httpErrorCode A custom Kalix status code to represent the error.
       * @param <R>           The type of the message that must be returned by this call.
       * @return An error reply.
       */
      <R> ErrorEffect<R> error(String description, StatusCode.ErrorCode httpErrorCode);
    }

    interface ErrorEffect<T> extends Effect<T> {
    }

    /**
     * A workflow effect type that contains information about the transition to the next step.
     * This could be also a special transition to pause or end the workflow.
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
       * Defines the next step to which the workflow should transition to.
       * <p>
       * The step definition identified by {@code stepName} must have an input parameter of type I.
       * In other words, the next step call (or asyncCall) must have been defined with a {@link Function} that
       * accepts an input parameter of type I.
       *
       * @param stepName The step name that should be executed next.
       * @param input    The input param for the next step.
       */
      @ApiMayChange
      <I> TransitionalEffect<Void> transitionTo(String stepName, I input);

      /**
       * Defines the next step to which the workflow should transition to.
       * <p>
       * The step definition identified by {@code stepName} must not have an input parameter.
       * In other words, the next step call (or asyncCall) must have been defined with a {@link Supplier}.
       *
       * @param stepName The step name that should be executed next.
       */
      @ApiMayChange
      TransitionalEffect<Void> transitionTo(String stepName);

      /**
       * Finish the workflow execution.
       * After transition to {@code end}, no more transitions are allowed.
       */
      @ApiMayChange
      TransitionalEffect<Void> end();
    }


  }

  public static class WorkflowDef<S> {

    final private List<Step> steps = new ArrayList<>();
    final private List<StepConfig> stepConfigs = new ArrayList<>();
    final private Set<String> uniqueNames = new HashSet<>();
    private Optional<Duration> workflowTimeout = Optional.empty();
    private Optional<String> failoverStepName = Optional.empty();
    private Optional<Object> failoverStepInput = Optional.empty();
    private Optional<MaxRetries> failoverMaxRetries = Optional.empty();
    private Optional<Duration> stepTimeout = Optional.empty();
    private Optional<RecoverStrategy<?>> stepRecoverStrategy = Optional.empty();


    private WorkflowDef() {
    }

    public Optional<Step> findByName(String name) {
      return steps.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    /**
     * Add step to workflow definition. Step name must be unique.
     *
     * @param step A workflow step
     */
    public WorkflowDef<S> addStep(Step step) {
      addStepWithValidation(step);
      return this;
    }

    /**
     * Add step to workflow definition with a dedicated {@link RecoverStrategy}. Step name must be unique.
     *
     * @param step            A workflow step
     * @param recoverStrategy A Step recovery strategy
     */
    public WorkflowDef<S> addStep(Step step, RecoverStrategy<?> recoverStrategy) {
      addStepWithValidation(step);
      stepConfigs.add(new StepConfig(step.name(), step.timeout(), Optional.of(recoverStrategy)));
      return this;
    }

    private void addStepWithValidation(Step step) {
      if (uniqueNames.contains(step.name()))
        throw new IllegalArgumentException("Name '" + step.name() + "' is already in use by another step in this workflow");

      this.steps.add(step);
      this.uniqueNames.add(step.name());
    }


    /**
     * Define a timeout for the duration of the entire workflow. When the timeout expires, the workflow is finished and no transitions are allowed.
     *
     * @param timeout Timeout duration
     */
    public WorkflowDef<S> timeout(Duration timeout) {
      this.workflowTimeout = Optional.of(timeout);
      return this;
    }

    /**
     * Define a failover step name after workflow timeout. Note that recover strategy for this step can set only the number of max retries.
     *
     * @param stepName   A failover step name
     * @param maxRetries A recovery strategy for failover step.
     */
    public WorkflowDef<S> failoverTo(String stepName, MaxRetries maxRetries) {
      this.failoverStepName = Optional.of(stepName);
      this.failoverMaxRetries = Optional.of(maxRetries);
      return this;
    }

    /**
     * Define a failover step name after workflow timeout. Note that recover strategy for this step can set only the number of max retries.
     *
     * @param stepName   A failover step name
     * @param stepInput  A failover step input
     * @param maxRetries A recovery strategy for failover step.
     */
    public <I> WorkflowDef<S> failoverTo(String stepName, I stepInput, MaxRetries maxRetries) {
      this.failoverStepName = Optional.of(stepName);
      this.failoverStepInput = Optional.of(stepInput);
      this.failoverMaxRetries = Optional.of(maxRetries);
      return this;
    }

    /**
     * Define a default step timeout. If not set, a default value of 5 seconds is used.
     * Can be overridden with step configuration.
     */
    public WorkflowDef<S> defaultStepTimeout(Duration timeout) {
      this.stepTimeout = Optional.of(timeout);
      return this;
    }

    /**
     * Define a default step recovery strategy. Can be overridden with step configuration.
     */
    public WorkflowDef<S> defaultStepRecoverStrategy(RecoverStrategy recoverStrategy) {
      this.stepRecoverStrategy = Optional.of(recoverStrategy);
      return this;
    }

    public Optional<Duration> getWorkflowTimeout() {
      return workflowTimeout;
    }

    public Optional<Duration> getStepTimeout() {
      return stepTimeout;
    }

    public Optional<RecoverStrategy<?>> getStepRecoverStrategy() {
      return stepRecoverStrategy;
    }

    public List<Step> getSteps() {
      return steps;
    }

    public List<StepConfig> getStepConfigs() {
      return stepConfigs;
    }

    public Optional<String> getFailoverStepName() {
      return failoverStepName;
    }

    public Optional<?> getFailoverStepInput() {
      return failoverStepInput;
    }

    public Optional<MaxRetries> getFailoverMaxRetries() {
      return failoverMaxRetries;
    }
  }


  public WorkflowDef<S> workflow() {
    return new WorkflowDef<>();
  }


  public interface Step {
    String name();

    Optional<Duration> timeout();


  }

  public static class CallStep<CallInput, DefCallInput, DefCallOutput, FailoverInput> implements Step {

    final private String _name;
    final public Function<CallInput, DeferredCall<DefCallInput, DefCallOutput>> callFunc;
    final public Function<DefCallOutput, Effect.TransitionalEffect<Void>> transitionFunc;
    final public Class<CallInput> callInputClass;
    final public Class<DefCallOutput> transitionInputClass;
    private Optional<Duration> _timeout = Optional.empty();

    public CallStep(String name,
                    Class<CallInput> callInputClass,
                    Function<CallInput, DeferredCall<DefCallInput, DefCallOutput>> callFunc,
                    Class<DefCallOutput> transitionInputClass,
                    Function<DefCallOutput, Effect.TransitionalEffect<Void>> transitionFunc) {
      _name = name;
      this.callInputClass = callInputClass;
      this.callFunc = callFunc;
      this.transitionInputClass = transitionInputClass;
      this.transitionFunc = transitionFunc;
    }

    @Override
    public String name() {
      return this._name;
    }

    @Override
    public Optional<Duration> timeout() {
      return this._timeout;
    }

    /**
     * Define a step timeout.
     */
    public CallStep<CallInput, DefCallInput, DefCallOutput, FailoverInput> timeout(Duration timeout) {
      this._timeout = Optional.of(timeout);
      return this;
    }
  }

  public static class AsyncCallStep<CallInput, CallOutput, FailoverInput> implements Step {

    final private String _name;
    final public Function<CallInput, CompletionStage<CallOutput>> callFunc;
    final public Function<CallOutput, Effect.TransitionalEffect<Void>> transitionFunc;
    final public Class<CallInput> callInputClass;
    final public Class<CallOutput> transitionInputClass;
    private Optional<Duration> _timeout = Optional.empty();

    public AsyncCallStep(String name,
                         Class<CallInput> callInputClass,
                         Function<CallInput, CompletionStage<CallOutput>> callFunc,
                         Class<CallOutput> transitionInputClass,
                         Function<CallOutput, Effect.TransitionalEffect<Void>> transitionFunc) {
      _name = name;
      this.callInputClass = callInputClass;
      this.callFunc = callFunc;
      this.transitionInputClass = transitionInputClass;
      this.transitionFunc = transitionFunc;
    }

    @Override
    public String name() {
      return this._name;
    }

    @Override
    public Optional<Duration> timeout() {
      return this._timeout;
    }

    /**
     * Define a step timeout.
     */
    public AsyncCallStep<CallInput, CallOutput, FailoverInput> timeout(Duration timeout) {
      this._timeout = Optional.of(timeout);
      return this;
    }
  }

  public static class StepConfig {
    public final String stepName;
    public final Optional<Duration> timeout;
    public final Optional<RecoverStrategy<?>> recoverStrategy;

    public StepConfig(String stepName, Optional<Duration> timeout, Optional<RecoverStrategy<?>> recoverStrategy) {
      this.stepName = stepName;
      this.timeout = timeout;
      this.recoverStrategy = recoverStrategy;
    }
  }

  /**
   * Starts defining a recover strategy for the workflow or a specific step.
   *
   * @param maxRetries number of retries before giving up.
   * @return MaxRetries strategy.
   */
  public MaxRetries maxRetries(int maxRetries) {
    return RecoverStrategy.maxRetries(maxRetries);
  }

  public static class RecoverStrategy<T> {

    public final int maxRetries;
    public final String failoverStepName;
    public final Optional<T> failoverStepInput;

    public RecoverStrategy(int maxRetries, String failoverStepName, Optional<T> failoverStepInput) {
      this.maxRetries = maxRetries;
      this.failoverStepName = failoverStepName;
      this.failoverStepInput = failoverStepInput;
    }

    /**
     * Retry strategy without failover configuration
     */
    public static class MaxRetries {
      public final int maxRetries;

      public MaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
      }

      /**
       * Once max retries is exceeded, transition to a given step name.
       */
      public RecoverStrategy<?> failoverTo(String stepName) {
        return new RecoverStrategy<>(maxRetries, stepName, Optional.<Void>empty());
      }

      /**
       * Once max retries is exceeded, transition to a given step name with the input parameter.
       */
      public <T> RecoverStrategy<T> failoverTo(String stepName, T input) {
        return new RecoverStrategy<>(maxRetries, stepName, Optional.of(input));
      }

      public int getMaxRetries() {
        return maxRetries;
      }
    }

    /**
     * Set the number of retires for a failed step, <code>maxRetries</code> equals 0 means that the step won't retry in case of failure.
     */
    public static MaxRetries maxRetries(int maxRetries) {
      return new MaxRetries(maxRetries);
    }

    /**
     * In case of a step failure don't retry but transition to a given step name.
     */
    public static RecoverStrategy<?> failoverTo(String stepName) {
      return new RecoverStrategy<>(0, stepName, Optional.<Void>empty());
    }

    /**
     * In case of a step failure don't retry but transition to a given step name with the input parameter.
     */
    public static <T> RecoverStrategy<T> failoverTo(String stepName, T input) {
      return new RecoverStrategy<>(0, stepName, Optional.of(input));
    }
  }

}
