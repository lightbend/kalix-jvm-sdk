package kalix;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Goal which deploys the current project to the repository and Kalix.
 */
@SuppressWarnings("unused")
@Mojo(name = "deploy")
public class DeployMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.basedir}", property = "baseDir", required = true)
    private File baseDir;

    @Parameter(property = "dockerImage", required = true)
    private String dockerImage;

    @Parameter(defaultValue = "${project.artifactId}", property = "service", required = true)
    private String service;

    @Parameter(defaultValue = "kalix", property = "kalixPath", required = true)
    private String kalixPath;

    @Parameter(property = "kalixContext")
    private String kalixContext;

    @Parameter(property = "kalixProject")
    private String kalixProject;

    @Parameter(defaultValue = "30000", property = "cliTimeoutMs", required = true)
    private Long cliTimeoutMs;

    private final Log log = getLog();

    /**
     * Deploys to Kalix if set to 'kalixProject' by invoking the `kalix services deploy` command.
     * If 'kalixProject' is not set then deploying to the currently selected project.
     */
    public void execute() throws MojoExecutionException {
       deploy(service, dockerImage,Optional.ofNullable(kalixProject),Optional.ofNullable(kalixContext));
    }

    private void deploy(String service, String dockerImage, Optional<String> kalixProject, Optional<String> kalixContext) throws MojoExecutionException {
        try {
            List<String> commandLine = new ArrayList<>(List.of(kalixPath, "service", "deploy", service, dockerImage));
            String messageExtraInfo = "";

            if (kalixProject.isPresent()){
                commandLine.add("--project");
                commandLine.add(kalixProject.get());
                messageExtraInfo += " in project ["+kalixProject.get()+"]";
            } else {
                log.info("`kalixProject` hasn't been set. Therefore, deploying to the currently selected project configured via Kalix CLI.");
            }
            if (kalixContext.isPresent()){
                commandLine.add( "--context");
                commandLine.add(kalixContext.get());
                messageExtraInfo += " with context ["+kalixContext.get()+"]";
            }

            log.info("Deploying project to Kalix.");
            String commandLineString = String.join(" ", commandLine);
            log.info("Executing `" + commandLineString + "`.");
            Process process = new ProcessBuilder().directory(baseDir).command(commandLine).start();
            synchronized (process) {
                process.wait(cliTimeoutMs);
            }
            final int deploymentResult = process.exitValue();
            if (deploymentResult == 0) {
                log.info("Successfully deployed service ["+service+"]" + messageExtraInfo + ".");
            } else {
                InputStream errorStream = process.getErrorStream();
                Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8.name());
                log.error("Unable to deploy. Error executing `" + commandLineString + "`.");
                while(scanner.hasNext()){
                    log.error(scanner.useDelimiter("\\A").next().replaceAll("[\\n\\r]", ""));
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("There was a problem deploying", e);
        }
    }
}
