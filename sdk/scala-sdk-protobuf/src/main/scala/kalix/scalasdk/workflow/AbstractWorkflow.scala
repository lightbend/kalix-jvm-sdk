/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.workflow

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import akka.annotation.ApiMayChange
import io.grpc.Status
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.workflow.WorkflowEffectImpl
import kalix.scalasdk.timer.TimerScheduler
import kalix.scalasdk.workflow.AbstractWorkflow.RecoverStrategy.MaxRetries

object AbstractWorkflow {

  object Effect {

    /**
     * Construct the effect that is returned by the command handler or a step transition.
     *
     * The effect describes next processing actions, such as updating state, transition to another step and sending a
     * reply.
     *
     * @tparam S
     *   The type of the state for this workflow.
     */
    trait Builder[S] {
      @ApiMayChange
      def updateState(newState: S): PersistenceEffectBuilder[S]

      /**
       * Pause the workflow execution and wait for an external input, e.g. via command handler.
       */
      @ApiMayChange
      def pause: TransitionalEffect[Void]

      /**
       * Defines the next step to which the workflow should transition to.
       *
       * The step definition identified by `stepName` must have an input parameter of type I. In other words, the next
       * step call (or asyncCall) must have been defined with a function that accepts an input parameter of type I.
       *
       * @param stepName
       *   The step name that should be executed next.
       * @param input
       *   The input param for the next step.
       * @tparam I
       *   The input param type for the next step.
       */
      @ApiMayChange
      def transitionTo[I](stepName: String, input: I): TransitionalEffect[Void]

      /**
       * Defines the next step to which the workflow should transition to.
       *
       * The step definition identified by `stepName` must not have an input parameter. In other words, the next step
       * call (or asyncCall) must have been defined with a parameters less function.
       *
       * @param stepName
       *   The step name that should be executed next.
       */
      @ApiMayChange
      def transitionTo(stepName: String): TransitionalEffect[Void]

      /**
       * Finish the workflow execution. After transition to `end`, no more transitions are allowed.
       */
      @ApiMayChange
      def end: TransitionalEffect[Void]

      /**
       * Create a message reply.
       *
       * @param replyMessage
       *   The payload of the reply.
       * @tparam R
       *   The type of the message that must be returned by this call.
       * @return
       *   A message reply.
       */
      def reply[R](replyMessage: R): AbstractWorkflow.Effect[R]

      /**
       * Reply after for example `updateState`.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @tparam R
       *   The type of the message that must be returned by this call.
       * @return
       *   A message reply.
       */
      def reply[R](message: R, metadata: Metadata): AbstractWorkflow.Effect[R]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @tparam R
       *   The type of the message that must be returned by this call.
       * @return
       *   An error reply.
       */
      def error[R](description: String): ErrorEffect[R]

      /**
       * Create an error reply with a gRPC status code.
       *
       * @param description
       *   The description of the error.
       * @param statusCode
       *   A custom gRPC status code.
       * @tparam R
       *   The type of the message that must be returned by this call.
       * @return
       *   An error reply.
       */
      def error[R](description: String, statusCode: Status.Code): ErrorEffect[R]
    }

    trait ErrorEffect[T] extends AbstractWorkflow.Effect[T] {}

    /**
     * A workflow effect type that contains information about the transition to the next step. This could be also a
     * special transition to pause or end the workflow.
     */
    trait TransitionalEffect[T] extends AbstractWorkflow.Effect[T] {

      /**
       * Reply after for example `updateState`.
       *
       * @param message
       *   The payload of the reply.
       * @tparam R
       *   The type of the message that must be returned by this call.
       * @return
       *   A message reply.
       */
      def thenReply[R](message: R): AbstractWorkflow.Effect[R]

      /**
       * Reply after for example `updateState`.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @tparam R
       *   The type of the message that must be returned by this call.
       * @return
       *   A message reply.
       */
      def thenReply[R](message: R, metadata: Metadata): AbstractWorkflow.Effect[R]
    }

    trait PersistenceEffectBuilder[T] {

      /**
       * Pause the workflow execution and wait for an external input, e.g. via command handler.
       */
      @ApiMayChange
      def pause: TransitionalEffect[Void]

      /**
       * Defines the next step to which the workflow should transition to.
       *
       * The step definition identified by `stepName` must have an input parameter of type I. In other words, the next
       * step call (or asyncCall) must have been defined with a function that accepts an input parameter of type I.
       *
       * @param stepName
       *   The step name that should be executed next.
       * @param input
       *   The input param for the next step.
       */
      @ApiMayChange
      def transitionTo[I](stepName: String, input: I): TransitionalEffect[Void]

      /**
       * Defines the next step to which the workflow should transition to.
       *
       * The step definition identified by `stepName` must not have an input parameter. In other words, the next step
       * call (or asyncCall) must have been defined with a parameter less function.
       *
       * @param stepName
       *   The step name that should be executed next.
       */
      @ApiMayChange
      def transitionTo(stepName: String): TransitionalEffect[Void]

      /**
       * Finish the workflow execution. After transition to `end`, no more transitions are allowed.
       */
      @ApiMayChange
      def end: TransitionalEffect[Void]
    }

  }

  trait Effect[T] {}

  class WorkflowDef[S](
      private val _steps: ArrayBuffer[AbstractWorkflow.Step] = ArrayBuffer.empty,
      private val _stepConfigs: ArrayBuffer[AbstractWorkflow.StepConfig] = ArrayBuffer.empty,
      private val uniqueNames: mutable.Set[String] = mutable.Set.empty,
      private var _workflowTimeout: Option[FiniteDuration] = None,
      private var _failoverStepName: Option[String] = None,
      private var _failoverStepInput: Option[Any] = None,
      private var _failoverMaxRetries: Option[MaxRetries] = None,
      private var _stepTimeout: Option[FiniteDuration] = None,
      private var _stepRecoverStrategy: Option[AbstractWorkflow.RecoverStrategy[_]] = None) {

    def findByName(name: String): Option[AbstractWorkflow.Step] = _steps.find(_.name == name)

    /**
     * Add step to workflow definition. Step name must be unique.
     *
     * @param step
     *   A workflow step
     */
    def addStep(step: AbstractWorkflow.Step): AbstractWorkflow.WorkflowDef[S] = {
      addStepWithValidation(step)
      this
    }

    /**
     * Add step to workflow definition with a dedicated [[RecoverStrategy]]. Step name must be unique.
     *
     * @param step
     *   A workflow step
     * @param recoverStrategy
     *   A Step recovery strategy
     */
    def addStep(
        step: AbstractWorkflow.Step,
        recoverStrategy: AbstractWorkflow.RecoverStrategy[_]): AbstractWorkflow.WorkflowDef[S] = {
      addStepWithValidation(step)
      _stepConfigs.addOne(AbstractWorkflow.StepConfig(step.name, step.timeout, Option(recoverStrategy)))
      this
    }

    private def addStepWithValidation(step: AbstractWorkflow.Step): Unit = {
      if (uniqueNames.contains(step.name))
        throw new IllegalArgumentException(
          "Name '" + step.name + "' is already in use by another step in this workflow")
      this._steps.addOne(step)
      this.uniqueNames.add(step.name)
    }

    /**
     * Define a timeout for the duration of the entire workflow. When the timeout expires, the workflow is finished and
     * no transitions are allowed.
     *
     * @param timeout
     *   Timeout duration
     */
    def timeout(timeout: FiniteDuration): AbstractWorkflow.WorkflowDef[S] = {
      this._workflowTimeout = Option(timeout)
      this
    }

    /**
     * Define a failover step name after workflow timeout. Note that recover strategy for this step can set only the
     * number of max retries.
     *
     * @param stepName
     *   A failover step name
     * @param maxRetries
     *   A recovery strategy for failover step.
     */
    def failoverTo(stepName: String, maxRetries: MaxRetries): AbstractWorkflow.WorkflowDef[S] = {
      if (stepName == null) throw new IllegalArgumentException("Step name cannot be null")
      if (maxRetries == null) throw new IllegalArgumentException("Max retries cannot be null")
      this._failoverStepName = Option(stepName)
      this._failoverMaxRetries = Option(maxRetries)
      this
    }

    /**
     * Define a failover step name after workflow timeout. Note that recover strategy for this step can set only the
     * number of max retries.
     *
     * @param stepName
     *   A failover step name
     * @param stepInput
     *   A failover step input
     * @param maxRetries
     *   A recovery strategy for failover step.
     */
    def failoverTo[I](stepName: String, stepInput: I, maxRetries: MaxRetries): AbstractWorkflow.WorkflowDef[S] = {
      if (stepName == null) throw new IllegalArgumentException("Step name cannot be null")
      if (stepInput == null) throw new IllegalArgumentException("Step input cannot be null")
      if (maxRetries == null) throw new IllegalArgumentException("Max retries cannot be null")
      this._failoverStepName = Option(stepName)
      this._failoverStepInput = Option(stepInput)
      this._failoverMaxRetries = Option(maxRetries)
      this
    }

    /**
     * Define a default step timeout. If not set, a default value of 5 seconds is used. Can be overridden with step
     * configuration.
     */
    def defaultStepTimeout(timeout: FiniteDuration): AbstractWorkflow.WorkflowDef[S] = {
      this._stepTimeout = Option(timeout)
      this
    }

    /**
     * Define a default step recovery strategy. Can be overridden with step configuration.
     */
    def defaultStepRecoverStrategy(
        recoverStrategy: AbstractWorkflow.RecoverStrategy[_]): AbstractWorkflow.WorkflowDef[S] = {
      this._stepRecoverStrategy = Option(recoverStrategy)
      this
    }

    def workflowTimeout: Option[FiniteDuration] = _workflowTimeout

    def stepTimeout: Option[FiniteDuration] = _stepTimeout

    def stepRecoverStrategy: Option[AbstractWorkflow.RecoverStrategy[_]] = _stepRecoverStrategy

    def steps: List[AbstractWorkflow.Step] = _steps.toList

    def stepConfigs: List[AbstractWorkflow.StepConfig] = _stepConfigs.toList

    def failoverStepName: Option[String] = _failoverStepName

    def failoverStepInput: Option[_] = _failoverStepInput

    def failoverMaxRetries: Option[MaxRetries] = _failoverMaxRetries
  }

  sealed trait Step {
    def name: String

    def timeout: Option[FiniteDuration]
  }

  case class CallStep[CallInput, DefCallInput, DefCallOutput, FailoverInput](
      private val _name: String,
      callInputClass: Class[CallInput],
      callFunc: CallInput => DeferredCall[DefCallInput, DefCallOutput],
      transitionInputClass: Class[DefCallOutput],
      transitionFunc: DefCallOutput => Effect.TransitionalEffect[Void])
      extends AbstractWorkflow.Step {
    private var _timeout: Option[FiniteDuration] = Option.empty

    override def name: String = this._name

    override def timeout: Option[FiniteDuration] = this._timeout

    /**
     * Define a step timeout.
     */
    def timeout(
        timeout: FiniteDuration): AbstractWorkflow.CallStep[CallInput, DefCallInput, DefCallOutput, FailoverInput] = {
      this._timeout = Option(timeout)
      this
    }
  }

  class AsyncCallStep[CallInput, CallOutput, FailoverInput](
      private val _name: String,
      val callInputClass: Class[CallInput],
      val callFunc: CallInput => Future[CallOutput],
      val transitionInputClass: Class[CallOutput],
      val transitionFunc: CallOutput => Effect.TransitionalEffect[Void])
      extends AbstractWorkflow.Step {
    private var _timeout: Option[FiniteDuration] = Option.empty

    override def name: String = this._name

    override def timeout: Option[FiniteDuration] = this._timeout

    /**
     * Define a step timeout.
     */
    def timeout(timeout: FiniteDuration): AbstractWorkflow.AsyncCallStep[CallInput, CallOutput, FailoverInput] = {
      this._timeout = Option(timeout)
      this
    }
  }

  case class StepConfig(
      stepName: String,
      timeout: Option[FiniteDuration],
      recoverStrategy: Option[AbstractWorkflow.RecoverStrategy[_]]) {}

  /**
   * Starts defining a recover strategy for the workflow or a specific step.
   * @param maxRetries
   *   number of retries before giving up.
   */
  def maxRetries(maxRetries: Int): MaxRetries = RecoverStrategy.maxRetries(maxRetries)

  object RecoverStrategy {

    /**
     * Retry strategy without failover configuration
     */
    case class MaxRetries(maxRetries: Int) {

      /**
       * Once max retries is exceeded, transition to a given step name.
       */
      def failoverTo(stepName: String): AbstractWorkflow.RecoverStrategy[_] =
        new AbstractWorkflow.RecoverStrategy[Void](maxRetries, stepName, Option.empty)

      /**
       * Once max retries is exceeded, transition to a given step name with the input parameter.
       */
      def failoverTo[T](stepName: String, input: T): AbstractWorkflow.RecoverStrategy[T] = {
        if (input == null) throw new IllegalArgumentException("Input parameter cannot be null")
        new AbstractWorkflow.RecoverStrategy[T](maxRetries, stepName, Option(input))
      }
    }

    /**
     * Set the number of retires for a failed step, `maxRetries` equals 0 means that the step won't retry in case of
     * failure.
     */
    def maxRetries(maxRetries: Int): MaxRetries = MaxRetries(maxRetries)

    /**
     * In case of a step failure don't retry but transition to a given step name.
     */
    def failoverTo(stepName: String): AbstractWorkflow.RecoverStrategy[_] =
      AbstractWorkflow.RecoverStrategy[Void](0, stepName, Option.empty)

    /**
     * In case of a step failure don't retry but transition to a given step name with the input parameter.
     */
    def failoverTo[T](stepName: String, input: T): AbstractWorkflow.RecoverStrategy[T] = {
      if (input == null) throw new IllegalArgumentException("Input parameter cannot be null")
      new AbstractWorkflow.RecoverStrategy[T](0, stepName, Option(input))
    }
  }

  case class RecoverStrategy[T](maxRetries: Int, failoverStepName: String, failoverStepInput: Option[T]) {}
}

abstract class AbstractWorkflow[S >: Null] {
  private var _commandContext: Option[CommandContext] = None
  private var _timerScheduler: Option[TimerScheduler] = None

  private var _currentState: Option[S] = None

  private var _stateHasBeenSet = false

  /**
   * Implement by returning the initial empty state object. This object will be passed into the command handlers, until
   * a new state replaces it.
   *
   * Also known as "zero state" or "neutral state".
   *
   * `null` is an allowed value.
   */
  def emptyState: S

  /**
   * Returns the state as currently stored by Kalix.
   *
   * Note that modifying the state directly will not update it in storage. To save the state, one must call
   * `effects.updateState()`.
   *
   * This method can only be called when handling a command. Calling it outside a method (eg: in the constructor) will
   * raise a IllegalStateException exception.
   *
   * @throws java.lang.IllegalStateException
   *   if accessed outside a handler method
   */
  @ApiMayChange
  protected def currentState(): S = {
    // user may call this method inside a command handler and get a null because it's legal
    // to have emptyState set to null.
    if (_stateHasBeenSet) _currentState.orNull
    else throw new IllegalStateException("Current state is only available when handling a command.")
  }

  /**
   * Additional context and metadata for a command handler.
   *
   * It will throw an exception if accessed from constructor.
   */
  protected def commandContext(): CommandContext = {
    try {
      _commandContext.get
    } catch {
      case _: NoSuchElementException =>
        throw new IllegalStateException("CommandContext is only available when handling a command.")
    }
  }

  /**
   * INTERNAL API
   */
  def _internalSetCommandContext(context: Option[CommandContext]): Unit = {
    _commandContext = context
  }

  /**
   * INTERNAL API
   */
  def _internalSetTimerScheduler(timerScheduler: Option[TimerScheduler]): Unit = {
    this._timerScheduler = timerScheduler
  }

  /**
   * Returns a [[kalix.scalasdk.timer.TimerScheduler]] that can be used to schedule further in time.
   */
  def timers: TimerScheduler = {
    try {
      _timerScheduler.get
    } catch {
      case _: NoSuchElementException =>
        throw new IllegalStateException(
          "Timers can only be scheduled or cancelled when handling a command or running a step action.")
    }
  }

  /**
   * INTERNAL API
   */
  def _internalSetCurrentState(state: S): Unit = {
    _stateHasBeenSet = true
    _currentState = Option(state)
  }

  /**
   * @return
   *   A workflow definition in a form of steps and transitions between them.
   */
  @ApiMayChange
  def definition: AbstractWorkflow.WorkflowDef[S]

  protected def effects: AbstractWorkflow.Effect.Builder[S] = WorkflowEffectImpl()

  def workflow: AbstractWorkflow.WorkflowDef[S] = new AbstractWorkflow.WorkflowDef[S]()

}
