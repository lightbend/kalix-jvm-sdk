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

import kalix.javasdk.Kalix;
import kalix.javasdk.testkit.KalixTestKit;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;

class KalixTestKitExtension implements BeforeAllCallback, ParameterResolver {

  private static final Namespace NAMESPACE = Namespace.create(KalixTestKitExtension.class);
  private static final String TESTKIT = "testkit";

  @Override
  public void beforeAll(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    Kalix kalix = findKalixDescriptor(testClass);
    KalixTestKit testkit = new KalixTestKit(kalix).start();
    context.getStore(NAMESPACE).put(TESTKIT, new StoredTestkit(testkit));
  }

  private static Kalix findKalixDescriptor(final Class<?> testClass) {
    return ReflectionUtils.findFields(
            testClass,
            KalixTestKitExtension::isKalixDescriptor,
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
        .stream()
        .findFirst()
        .map(KalixTestKitExtension::getKalixDescriptor)
        .orElseThrow(
            () ->
                new ExtensionConfigurationException(
                    "No field annotated with @KalixDescriptor found for @KalixTest"));
  }

  private static boolean isKalixDescriptor(final Field field) {
    if (AnnotationSupport.isAnnotated(field, KalixDescriptor.class)) {
      if (Kalix.class.isAssignableFrom(field.getType())) {
        return true;
      } else {
        throw new ExtensionConfigurationException(
            String.format(
                "Field [%s] annotated with @KalixDescriptor is not a Kalix", field.getName()));
      }
    } else {
      return false;
    }
  }

  private static Kalix getKalixDescriptor(final Field field) {
    return (Kalix)
        ReflectionUtils.tryToReadFieldValue(field)
            .getOrThrow(
                e ->
                    new ExtensionConfigurationException(
                        "Cannot access Kalix defined in field " + field.getName(), e));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
    Class<?> type = parameterContext.getParameter().getType();
    return type == KalixTestKit.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
    Class<?> type = parameterContext.getParameter().getType();
    Store store = context.getStore(NAMESPACE);
    KalixTestKit testkit = store.get(TESTKIT, StoredTestkit.class).getTestkit();
    if (type == KalixTestKit.class) {
      return testkit;
    } else {
      throw new ParameterResolutionException("Unexpected parameter type " + type);
    }
  }

  // Wrap testkit in CloseableResource, auto-closed when test finishes (extension store is closed)
  private static class StoredTestkit implements Store.CloseableResource {
    private final KalixTestKit testkit;

    private StoredTestkit(KalixTestKit testkit) {
      this.testkit = testkit;
    }

    public KalixTestKit getTestkit() {
      return testkit;
    }

    @Override
    public void close() {
      testkit.stop();
    }
  }
}
