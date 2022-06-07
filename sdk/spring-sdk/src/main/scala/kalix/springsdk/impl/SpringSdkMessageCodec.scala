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

package kalix.springsdk.impl

import com.google.protobuf.any
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.MessageCodec

object SpringSdkMessageCodec {
  def instance: SpringSdkMessageCodec = apply()
  def apply(): SpringSdkMessageCodec = new SpringSdkMessageCodec
}

class SpringSdkMessageCodec extends MessageCodec {

  /**
   * In the Spring SDK, output data are encoded to Json.
   */
  override def encodeScala(value: Any): any.Any =
    any.Any.fromJavaProto(JsonSupport.encodeJson(value))

  /**
   * In the Spring SDK, input data are kept as proto Any and delivered as such to the router
   */
  override def decodeMessage(value: any.Any): Any = value
}
