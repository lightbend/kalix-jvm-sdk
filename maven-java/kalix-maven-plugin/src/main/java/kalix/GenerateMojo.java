package kalix;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import kalix.codegen.DescriptorSet;
import kalix.codegen.java.ProtoMessageTypeExtractor$;
import kalix.codegen.java.SourceGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;

import kalix.codegen.Log;
import kalix.codegen.ModelBuilder;
import scala.collection.Iterable;
import scala.collection.immutable.List$;
import scala.util.Either;

/**
 * Goal which reads in protobuf files and produces entities given their
 * commands, events and states. Entities are produced in the source file
 * directory for Java, unless they already exist, in which case they will be
 * modified appropriately. Only type declarations associated with commands,
 * events and state are affected i.e. not the body of existing methods.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.basedir}", property = "baseDir", required = true)
    private File baseDir;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "user-function.desc", required = true)
    private String descriptorSetFileName;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/protobuf/descriptor-sets", required = true)
    private File descriptorSetOutputDirectory;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/kalix/java", required = true)
    private File generatedSourceDirectory;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/kalix/java", required = true)
  private File generatedTestSourceDirectory;

    // src/main/java
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "sourceDirectory", required = true)
    private File sourceDirectory;

    // src/test/java
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", property = "testSourceDirectory", required = true)
    private File testSourceDirectory;

    // src/test/java
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", property = "integrationTestSourceDirectory", required = true)
    private File integrationTestSourceDirectory;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.groupId}.Main", property = "mainClass", required = true)
    private String mainClass;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = ".*ServiceEntity", property = "serviceNamesFilter", required = true)
    private String serviceNamesFilter;

    private final Log log = new Log() {
      @Override
      public void debug(String message) { getLog().debug(message); }

      @Override
      public void warn(String message) {
        getLog().warn(message);
      }
      @Override
      public void info(String message) { getLog().info(message); }
    };

    /**
     * Given a protobuf descriptor, we inspect it and search for entities, commands,
     * events and state declarations, storing them in an appropriate structure. That
     * structure then drives the code generation phase.
     */
    public void execute() throws MojoExecutionException {
        File protobufDescriptor = descriptorSetOutputDirectory.toPath().resolve(descriptorSetFileName).toFile();
        if (protobufDescriptor.exists()) {
            log.info("Inspecting proto file descriptor for entity generation...");
            Either<DescriptorSet.CannotOpen, Either<DescriptorSet.ReadFailure, Iterable<Descriptors.FileDescriptor>>> descriptors = DescriptorSet
                    .fileDescriptors(protobufDescriptor);
            if (descriptors.isRight()) {
                Either<DescriptorSet.ReadFailure, Iterable<Descriptors.FileDescriptor>> protoFile = descriptors.right().get();
                if (protoFile.isRight()){
                  Iterable<FileDescriptor> fileDescriptors = protoFile.right().get();
                  // additionalDescriptors is empty, because problems with missing descriptors for imports
                  // can be solved by `includeDependenciesInDescriptorSet` flag, more: https://www.xolstice.org/protobuf-maven-plugin/usage.html
                  ModelBuilder.Model model = ModelBuilder.introspectProtobufClasses(fileDescriptors, List$.MODULE$.empty(), log, ProtoMessageTypeExtractor$.MODULE$);
                  log.debug("Model: " + model);
                  Iterable<Path> generated = SourceGenerator.generate(
                          model,
                          sourceDirectory.toPath(),
                          testSourceDirectory.toPath(),
                          integrationTestSourceDirectory.toPath(),
                          generatedSourceDirectory.toPath(),
                          generatedTestSourceDirectory.toPath(),
                          mainClass);
                  Path absBaseDir = baseDir.toPath().toAbsolutePath();
                  generated.foreach(p -> {
                      log.info("Generated: " + absBaseDir.relativize(p.toAbsolutePath()));
                      return null;
                  });

                  project.addCompileSourceRoot(generatedSourceDirectory.toString());
                  project.addTestCompileSourceRoot(generatedTestSourceDirectory.toString());

                } else {
                        throw new RuntimeException(new MojoExecutionException(
                        "There was a problem building the file descriptor from its protobuf: "
                            + descriptors.left().get().toString()));
                }
            } else {
                throw new MojoExecutionException("There was a problem opening the protobuf descriptor file",
                        descriptors.left().get().e());
            }
        } else {
            log.info("Skipping generation because there is no protobuf descriptor found.");
        }
    }
}
