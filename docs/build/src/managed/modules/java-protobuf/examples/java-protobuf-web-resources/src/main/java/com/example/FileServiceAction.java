package com.example;

import com.google.api.HttpBody;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.ActionCreationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com.example/web_resources.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FileServiceAction extends AbstractFileServiceAction {

  private String defaultContentType = "text/html";
  private Map<String, String> contentTypes = new HashMap();
  private static final Logger LOG = LoggerFactory.getLogger(FileServiceAction.class);
  private String baseDir = "/web";

  public FileServiceAction(ActionCreationContext creationContext) {
    contentTypes.put(".js","text/javascript");
    contentTypes.put(".css","text/css");
    contentTypes.put(".png","image/png");
  }

  private String getContentTypeByFile(String fileName) {
    for (Map.Entry<String, String> entry : contentTypes.entrySet()) {
      if(fileName.endsWith(entry.getKey())){
        return entry.getValue();
      }
    }
    return defaultContentType;
  }

  private  Effect<HttpBody> loadFile(String dir, String file) {
    String fullPath = baseDir+dir+"/"+file;
    try {
      // tag::200-ok[]
      InputStream inputStream = getClass().getResourceAsStream(fullPath);
      if(null == inputStream) {
        throw new NoSuchFileException("File " + fullPath + " not found");
      }
      byte[] byteArray = inputStream.readAllBytes();
      String contentType = getContentTypeByFile(file);
      HttpBody response = HttpBody.newBuilder()
              .setContentType(contentType)
              .setData(ByteString.copyFrom(byteArray))
              .build();
      LOG.info("Serving {} with {}", fullPath, contentType);
      Metadata header = Metadata.EMPTY.add("Cache-Control", "no-cache");
      return effects().reply(response, header);
      // end::200-ok[]
    } catch (NoSuchFileException e) {
      LOG.info("404: File {} not does not exist", fullPath);
      // tag::404-not-found[]
      return effects().ignore();
      // end::404-not-found[]
    } catch (IOException e) {
      LOG.error("500: Not able to serve {}", fullPath, e);
      // tag::500-error[]
      return effects().error("500: Not able to serve {}" + fullPath);
      // end::500-error[]
    }
  }

  @Override
  public Effect<HttpBody> getFile(WebResources.File file) {
    return loadFile("", file.getFile());
  }
  @Override
  public Effect<HttpBody> getFileInDir(WebResources.FileInDir fileInDir) {
    return loadFile("/"+fileInDir.getDirectory(), fileInDir.getFile());
  }

  @Override
  public Effect<HttpBody> indexHtml(Empty empty) {
    return loadFile("", "index.html");
  }
}
