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

package com.akkaserverless;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;

import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GenerateMojoTest {
  @Rule
  public MojoRule rule =
      new MojoRule() {
        @Override
        protected void before() {}

        @Override
        protected void after() {}
      };

  @Test
  public void testGeneration() throws Exception {
    Path projectDirectory = Paths.get("target/test-classes/project-to-test/");
    assertTrue(projectDirectory.toFile().exists());

    FileUtils.deleteDirectory(projectDirectory.resolve("src").toFile());
    FileUtils.deleteDirectory(projectDirectory.resolve("target").toFile());

    GenerateMojo myMojo =
        (GenerateMojo) rule.lookupConfiguredMojo(projectDirectory.toFile(), "generate");
    myMojo.execute();

    assertTrue(
        projectDirectory
            .resolve("src/main/java/com/example/shoppingcart/domain/ShoppingCartImpl.java")
            .toFile()
            .exists());
    assertTrue(
        projectDirectory
            .resolve(
                "target/generated-sources/akkaserverless/java/com/example/shoppingcart/domain/ShoppingCartInterface.java")
            .toFile()
            .exists());
    assertTrue(
        projectDirectory
            .resolve("src/test/java/com/example/shoppingcart/domain/ShoppingCartTest.java")
            .toFile()
            .exists());
    assertTrue(
        projectDirectory
            .resolve("src/it/java/com/example/shoppingcart/domain/ShoppingCartIntegrationTest.java")
            .toFile()
            .exists());
    assertTrue(
        projectDirectory
            .resolve(
                "target/generated-sources/akkaserverless/java/com/lightbend/MainComponentRegistrations.java")
            .toFile()
            .exists());

    assertTrue(projectDirectory.resolve("src/main/java/com/lightbend/Main.java").toFile().exists());
  }
}
