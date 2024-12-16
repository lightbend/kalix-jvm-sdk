/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.workflow

import java.time.Duration
import java.util.concurrent.CompletableFuture

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.CollectionHasAsScala

import com.google.protobuf.empty.Empty
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MetadataImpl
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.AbstractWorkflow.maxRetries
import kalix.scalasdk.workflow.ProtoWorkflow
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JavaWorkflowAdapterSpec extends AnyWordSpec with Matchers {

  "JavaWorkflowAdapter" should {
    "convert scala workflow definition to java" in {
      val adapted = new JavaWorkflowAdapter(new DummyWorkflow)

      val definition = adapted.definition()
      val steps = definition.getSteps.asScala
      steps should have size 2
      val step1 = steps.find(_.name() == "step1").get
      val step2 = steps.find(_.name() == "step2").get
      step1.timeout() shouldBe empty
      step2.timeout().get() shouldBe Duration.ofSeconds(10)

      val stepConfigs = definition.getStepConfigs.asScala
      val step1Config = stepConfigs.find(_.stepName == "step1").get
      val step2Config = stepConfigs.find(_.stepName == "step2").get

      step1Config.recoverStrategy.get().maxRetries shouldBe 2
      step1Config.recoverStrategy.get().failoverStepName shouldBe "step2"
      step1Config.timeout.isPresent shouldBe false

      step2Config.recoverStrategy.isPresent shouldBe false
      step2Config.timeout.get() shouldBe Duration.ofSeconds(10)

      definition.getStepTimeout().get() shouldBe Duration.ofSeconds(3)
      definition.getStepRecoverStrategy.get().maxRetries shouldBe 3
      definition.getStepRecoverStrategy.get().failoverStepName shouldBe "step1"
      definition.getFailoverStepName.get() shouldBe "step3"
      definition.getFailoverMaxRetries.get().maxRetries shouldBe 10
      definition.getWorkflowTimeout.get() shouldBe Duration.ofSeconds(7)
    }
  }
}

class DummyWorkflow extends ProtoWorkflow[Empty] {
  override def emptyState: Empty = Empty()

  override def definition: AbstractWorkflow.WorkflowDef[Empty] = {
    val step1 = step("step1")
      .call { _: Empty =>
        ScalaDeferredCallAdapter(
          GrpcDeferredCall(
            Empty(),
            MetadataImpl.Empty,
            "service1",
            "method1",
            _ => CompletableFuture.completedFuture(Empty())))
      }
      .andThen(_ => effects.end)

    val step2 = step("step2")
      .asyncCall { _: Empty =>
        Future.successful(Empty())
      }
      .andThen(_ => effects.end)
      .timeout(10.seconds)

    workflow
      .timeout(7.seconds)
      .defaultStepTimeout(3.seconds)
      .defaultStepRecoverStrategy(maxRetries(3).failoverTo("step1"))
      .failoverTo("step3", maxRetries(10))
      .addStep(step1, maxRetries(2).failoverTo("step2"))
      .addStep(step2)
  }
}
