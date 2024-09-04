package com.example;

import com.google.api.HttpBody;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FileServiceActionTest {

  @Test
  public void getFileTest() throws IOException {
    FileServiceActionTestKit service = FileServiceActionTestKit.of(FileServiceAction::new);
    HttpBody result = service.getFile(WebResources.File.newBuilder().setFile("index.html").build()).getReply();
    assertEquals(result, HttpBody.newBuilder().setData(
            ByteString.copyFrom(Files.readAllBytes(Paths.get("src/main/resources/web/index.html")))
            ).setContentType("text/html").build()
    );
  }

  @Test
  public void getFileNotFoundTest() throws IOException {
    FileServiceActionTestKit service = FileServiceActionTestKit.of(FileServiceAction::new);
    boolean result = service.getFile(WebResources.File.newBuilder().setFile("index_old.html").build()).isIgnore();
    assertEquals(result, true);
  }

  @Test
  public void getFileInDirTest() throws IOException {
    FileServiceActionTestKit service = FileServiceActionTestKit.of(FileServiceAction::new);
    HttpBody result = service.getFileInDir(WebResources.FileInDir.newBuilder().setDirectory("img").setFile("favicon.png").build()).getReply();
    assertEquals(result, HttpBody.newBuilder().setData(
                    ByteString.copyFrom(Files.readAllBytes(Paths.get("src/main/resources/web/img/favicon.png")))
            ).setContentType("image/png").build()
    );
  }

}
