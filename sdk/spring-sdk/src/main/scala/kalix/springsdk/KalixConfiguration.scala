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

package kalix.springsdk

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kalix.springsdk.impl.KalixServer
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class KalixConfiguration(applicationContext: ApplicationContext) {

  @Bean
  def config: Config = ConfigFactory.load()

  @Bean
  def kalixServer: KalixServer = {
    val kalix = new KalixServer(applicationContext, config)
    kalix.start()
    kalix
  }

  @Component
  class KalixComponentInjectionBlocker extends BeanPostProcessor {
    override def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
      if (KalixServer.kalixComponents.exists(_.isAssignableFrom(bean.getClass)))
        throw new IllegalArgumentException(
          s"${bean.getClass.getName} is Kalix component and is marked as a Spring bean for automatic wiring. " +
          "Kalix components cannot be accessed directly and therefore cannot be wired into other classes. " +
          "In order to interact with a Kalix component, you should call it using the provided KalixClient.")

      bean
    }
  }

}
