package kalix;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Goal which deploys the current project to Kalix.
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

    @Parameter(defaultValue = "kalix", property = "kalixPath", required = true)
    private String kalixPath;

    @Parameter(property = "kalixContext")
    private String kalixContext;

    @Parameter(property = "kalixCurrentProject", required = true)
    private String kalixCurrentProject;

    @Parameter(defaultValue = "false", property = "deployToKalix", required = true)
    private Boolean deployToKalix;

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
       if (!Boolean.valueOf(deployToKalix)){
           log.info("The variable `deployToKalix` hasn't been set to `true`. Therefore, not deploying to Kalix");
           log.info("To deploy to Kalix via `mvn deploy` you need to set `deployToKalix` to `true` " +
                   "and `kalixCurrentProject` to your current Kalix project Name (`kalix config get-project`)");
           return;
       }
       if (!currentKalixProjectMatches()){
           return;
       }
       if (kalixContext != null) {
           commandLine = Arrays.asList(kalixPath, "--context", kalixContext, "service", "deploy", service, dockerImage);
           deploy(commandLine);
           } else {
           commandLine = Arrays.asList(kalixPath, "service", "deploy", service, dockerImage);
           deploy(commandLine);
       }
    }

    private boolean currentKalixProjectMatches() throws MojoExecutionException {
      try {
          List<String> commandLine = Arrays.asList(kalixPath, "config", "get-project");
          Process process = new ProcessBuilder().command(commandLine).start();
          synchronized (process){
              process.wait(cliTimeoutMs);
          }
          final int getProjectResult = process.exitValue();
          if (getProjectResult == 0){
              InputStream inputStream = process.getInputStream();
              Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
              String currentProjectInfo = "";
              while(scanner.hasNext()) {
                  currentProjectInfo += scanner.next();
              }
              int newLine = currentProjectInfo.indexOf("\n");
              String titlesLine = currentProjectInfo.substring(0, newLine);
              String valuesLine = currentProjectInfo.substring(newLine + 1);
              String projectName = getName(titlesLine,valuesLine,1);

              if(!projectName.equals(kalixCurrentProject)){
                  log.info("Your current Kalix project Name is not [" + kalixCurrentProject + "]. Therefore, not deploying to Kalix");
                  log.info("Your current Kalix project (`kalix config get-project`) is:");
                  log.info(titlesLine);
                  log.info(valuesLine);
                  return false;
              }
              return true;
          } else {
              log.error("Unable to run `kalix config get-project`. Ensure you have installed Kalix CLI.");
              return false;
          }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("There was a problem deploying", e);
        }
    }

    private String getName(String titlesLine, String valuesLine, int expectedPosition) throws MojoExecutionException{
        //First checks that NAME is in the first position when `kalix config get-project`
        String nameTitle = Arrays.stream(titlesLine.split(" "))
                .filter( x -> !x.isEmpty()).collect(Collectors.toList()).get(1);
        if(nameTitle.equals("NAME")){
            String nameValue = Arrays.stream(valuesLine.split(" "))
                    .filter( x -> !x.isEmpty()).collect(Collectors.toList()).get(1);
            return nameValue;
        }else {
            throw new MojoExecutionException("Can't retrieve the NAME of your project. You probably have an incompatibility with the Kalix Maven plugin and the Kalix CLI.");
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
                log.error("Unable to deploy. Ensure you can deploy by using the kalix command line directly.");
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("There was a problem deploying", e);
        }
    }
}
