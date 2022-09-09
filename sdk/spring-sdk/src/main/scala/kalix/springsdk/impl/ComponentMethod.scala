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

import java.lang.reflect.Method

import com.google.protobuf.Descriptors
import kalix.springsdk.impl.reflection.ParameterExtractor

// Might need to have one of each of these for unary, streamed out, streamed in and streamed.
case class ComponentMethod(
    method: Option[Method],
    grpcMethodName: String,
    parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]],
    requestMessageDescriptor: Descriptors.Descriptor)
