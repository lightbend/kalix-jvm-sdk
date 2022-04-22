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

package kalix.javasdk.testkit.junit.jupiter;

import kalix.javasdk.testkit.KalixTestKit;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @KalixTest} registers a JUnit Jupiter extension to automatically manage the lifecycle of
 * {@link KalixTestKit}.
 *
 * <p><b>Note</b>: JUnit Jupiter is not provided as a transitive dependency of the Java SDK testkit
 * module but must be added explicitly to your project.
 *
 * <p>The kalix Testkit extension finds a field annotated with {@link KalixDescriptor} and creates a
 * testkit for this annotated {@code Kalix}.
 *
 * <p>The testkit can be injected into constructors or test methods, by specifying an {@code
 * KalixTestkit} parameter.
 *
 * @see KalixDescriptor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(KalixTestKitExtension.class)
@Inherited
public @interface KalixTest {}
