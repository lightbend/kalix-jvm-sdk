/*
 * Copyright 2024 Lightbend Inc.
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
