package com.lightbend;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GenerateMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() {
        }

        @Override
        protected void after() {
        }
    };

    @Test
    public void testGeneration() throws Exception {
        Path projectDirectory = Paths.get("target/test-classes/project-to-test/");
        assertTrue(projectDirectory.toFile().exists());

        FileUtils.deleteDirectory(projectDirectory.resolve("src/main/java").toFile());
        FileUtils.deleteDirectory(projectDirectory.resolve("target/classes").toFile());

        GenerateMojo myMojo = (GenerateMojo) rule.lookupConfiguredMojo(projectDirectory.toFile(), "generate");
        myMojo.execute();

        assertTrue(projectDirectory.resolve("src/main/java/com/lightbend/MyService.java").toFile().exists());
    }
}
