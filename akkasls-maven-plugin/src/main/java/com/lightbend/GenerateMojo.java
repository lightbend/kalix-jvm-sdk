package com.lightbend;

import com.google.protobuf.Descriptors;
import com.lightbend.akkasls.codegen.DescriptorSet;
import com.lightbend.akkasls.codegen.java.SourceGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Path;

import com.lightbend.akkasls.codegen.ModelBuilder;
import scala.collection.Iterable;
import scala.util.Either;

/**
 * Goal which reads in protobuf files and produces entities given their
 * commands, events and states. Entities are produced in the source file
 * directory for Java, unless they already exist, in which case they will be
 * modified appropriately. Only type declarations associated with commands,
 * events and state are affected i.e. not tbe body of existing methods.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateMojo extends AbstractMojo {
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.basedir}", property = "baseDir", required = true)
    private File baseDir;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.finalName}.protobin", required = true)
    private String descriptorSetFileName;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/protobuf/descriptor-sets", required = true)
    private File descriptorSetOutputDirectory;

    // src/main/java
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "sourceDirectory", required = true)
    private File sourceDirectory;

    // src/test/java
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", property = "testSourceDirectory", required = true)
    private File testSourceDirectory;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.groupId}.Main", property = "mainClass", required = true)
    private String mainClass;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = ".*ServiceEntity", property = "serviceNamesFilter", required = true)
    private String serviceNamesFilter;

    private final Log log = getLog();

    /**
     * Given a protobuf descriptor, we inspect it and search for entities, commands, events and
     * state declarations, storing them in an appropriate structure. That structure
     * then drives the code generation phase.
     */
    public void execute() throws MojoExecutionException {
        File protobufDescriptor = descriptorSetOutputDirectory.toPath().resolve(descriptorSetFileName).toFile();
        if (protobufDescriptor.exists()) {
            log.info("Inspecting proto file descriptor for entity generation...");
            Either<DescriptorSet.CannotOpen, Iterable<Either<DescriptorSet.ReadFailure, Descriptors.FileDescriptor>>> descriptors =
                    DescriptorSet.fileDescriptors(protobufDescriptor);
            if (descriptors.isRight()) {
                descriptors.right().get().iterator().foreach(descriptor -> {
                    if (descriptor.isRight()) {
                        Descriptors.FileDescriptor fileDescriptor = descriptor.right().get();
                        Iterable<ModelBuilder.Entity> entities = ModelBuilder.introspectProtobufClasses(fileDescriptor, serviceNamesFilter);
                        Iterable<Path> generated = SourceGenerator.generate(entities, sourceDirectory.toPath(), testSourceDirectory.toPath(), mainClass);
                        Path absBaseDir = baseDir.toPath().toAbsolutePath();
                        generated.foreach(p -> {
                            log.info("Generated: " + absBaseDir.relativize(p.toAbsolutePath()));
                            return null;
                        });

                    } else {
                        throw new RuntimeException(new MojoExecutionException("There was a problem building the file descriptor from its protobuf: " + descriptor.left().get().toString()));
                    }
                    return null;
                });

            } else {
                throw new MojoExecutionException("There was a problem opening the protobuf descriptor file", descriptors.left().get().e());
            }
        } else {
            log.info("Skipping generation because there is no protobuf descriptor found.");
        }
    }
}
