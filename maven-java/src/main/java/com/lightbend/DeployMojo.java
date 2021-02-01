package com.lightbend;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Goal which deploys the current project to Akka Serverless.
 */
@SuppressWarnings("unused")
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.basedir}", property = "baseDir", required = true)
    private File baseDir;

    @Parameter(property = "dockerImage", required = true)
    private String dockerImage;

    @Parameter(defaultValue = "${project.artifactId}", property = "service", required = true)
    private String service;

    @Parameter(defaultValue = "akkasls", property = "akkaslsPath", required = true)
    private String akkaslsPath;

    @Parameter(property = "akkaslsContext")
    private String akkaslsContext;

    @Parameter(defaultValue = "30000", property = "cliTimeoutMs", required = true)
    private Long cliTimeoutMs;

    private final Log log = getLog();

    /**
     * We deploy by invoking the services deploy command
     */
    public void execute() throws MojoExecutionException {
        log.info("Deploying project to Akka Serverless");
        try {
            final List<String> commandLine;
            if (akkaslsContext != null) {
                commandLine = Arrays.asList(akkaslsPath, "--context", akkaslsContext, "service", "deploy", service, dockerImage);
            } else {
                commandLine = Arrays.asList(akkaslsPath, "service", "deploy", service, dockerImage);
            }
            log.info("Executing `" + String.join(" ", commandLine) + "`");
            Process process = new ProcessBuilder().directory(baseDir).command(commandLine).start();
            synchronized (process) {
                process.wait(cliTimeoutMs);
            }
            int status = process.exitValue();
            if (status == 0) {
                log.info("Done.");
            } else {
                log.error("Unable to deploy. Ensure you can deploy by using the akkasls command line directly.");
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("There was a problem deploying", e);
        }
    }
}
