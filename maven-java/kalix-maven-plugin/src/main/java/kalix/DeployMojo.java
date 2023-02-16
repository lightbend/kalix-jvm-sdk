package kalix;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Goal which deploys the current project to Kalix.
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
     * We deploy by invoking the services deploy command only when `deployToKalix` is `true`
     * and the current Kalix project matches with 'kalixCurrentProject'.
     */
    public void execute() throws MojoExecutionException {
       final List<String> commandLine;
       final int deploymentResult = 0;
       if (kalixProject.isEmpty()){
           log.info("The variable `kalixProject` hasn't been set. Therefore, not deploying to Kalix");
           return;
       }
       if (kalixContext != null) {
           commandLine = Arrays.asList(kalixPath, "--context", kalixContext,"--project", kalixProject, "service", "deploy", service, dockerImage);
           deploy(commandLine);
           } else {
           commandLine = Arrays.asList(kalixPath,"--project", kalixProject, "service", "deploy", service, dockerImage);
           deploy(commandLine);
       }
    }

    private void deploy(List<String> commandLine) throws MojoExecutionException {
        try {
            log.info("Deploying project to Kalix");
            log.info("Executing `" + String.join(" ", commandLine) + "`");
            Process process = new ProcessBuilder().directory(baseDir).command(commandLine).start();
            synchronized (process) {
                process.wait(cliTimeoutMs);
            }
            final int deploymentResult = process.exitValue();
            if (deploymentResult == 0) {
                log.info("Done.");
            } else {
                InputStream errorStream = process.getErrorStream();
                Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8.name());
                while(scanner.hasNext()){
                    log.error(scanner.useDelimiter("\\A").next().replaceAll("[\\n\\r]", ""));
                }
                log.error("Unable to deploy. Ensure you can deploy by using the kalix command line directly.");
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("There was a problem deploying", e);
        }
    }
}
