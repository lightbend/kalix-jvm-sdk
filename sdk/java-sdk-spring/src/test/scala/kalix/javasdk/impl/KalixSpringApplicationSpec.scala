/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import scala.util.control.NonFatal

import kalix.spring.badwiring.action
import kalix.spring.badwiring.action.IllDefinedAction
import kalix.spring.badwiring.eventsourced.IllDefinedEventSourcedEntity
import kalix.spring.badwiring.eventsourced.Main
import kalix.spring.badwiring.valueentity
import kalix.spring.badwiring.valueentity.IllDefinedValueEntity
import kalix.spring.badwiring.view
import kalix.spring.badwiring.view.IllDefinedView
import kalix.spring.badwiring.workflow
import kalix.spring.badwiring.workflow.IllDefinedWorkflow
import kalix.spring.boot.KalixConfiguration
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.springframework.beans.factory.BeanCreationException

class KalixSpringApplicationSpec extends AnyWordSpec with Matchers {

  val message = "is a Kalix component and is marked as a Spring bean for automatic wiring."

  def tryCatch(f: () => Unit): Unit = {
    try {
      f()
    } catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
        throw ex
    }
  }
  "The KalixSpringApplication" should {

    "block direct wiring of Kalix Actions" in {
      val errorMessage =
        intercept[BeanCreationException] {
          tryCatch(() => action.Main.main(Array.empty))
        }.getCause.getMessage

      errorMessage shouldBe KalixConfiguration.beanPostProcessorErrorMessage(classOf[IllDefinedAction])
    }

    "block direct wiring of Kalix Event Sourced entities" in {
      val errorMessage =
        intercept[BeanCreationException] {
          tryCatch(() => Main.main(Array.empty))
        }.getCause.getMessage

      errorMessage shouldBe KalixConfiguration.beanPostProcessorErrorMessage(classOf[IllDefinedEventSourcedEntity])
    }

    "block direct wiring of Kalix Value entities" in {
      val errorMessage =
        intercept[BeanCreationException] {
          tryCatch(() => valueentity.Main.main(Array.empty))
        }.getCause.getMessage

      errorMessage shouldBe KalixConfiguration.beanPostProcessorErrorMessage(classOf[IllDefinedValueEntity])

    }

    "block direct wiring of Kalix Views" in {
      val errorMessage =
        intercept[BeanCreationException] {
          tryCatch(() => view.Main.main(Array.empty))
        }.getCause.getMessage

      errorMessage shouldBe KalixConfiguration.beanPostProcessorErrorMessage(classOf[IllDefinedView])
    }

    "block direct wiring of Kalix Workflows" in {
      val errorMessage =
        intercept[BeanCreationException] {
          tryCatch(() => workflow.Main.main(Array.empty))
        }.getCause.getMessage

      errorMessage shouldBe KalixConfiguration.beanPostProcessorErrorMessage(classOf[IllDefinedWorkflow])
    }
  }

}
