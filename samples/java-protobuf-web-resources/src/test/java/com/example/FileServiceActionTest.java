package com.example;

import akka.stream.javadsl.Source;
import com.example.FileAction;
import com.example.FileServiceAction;
import com.example.FileServiceActionTestKit;
import com.google.api.HttpBody;
import com.google.protobuf.ByteString;
import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.ActionResult;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FileServiceActionTest {

  @Test
  public void getFileTest() throws IOException {
    FileServiceActionTestKit service = FileServiceActionTestKit.of(FileServiceAction::new);
    HttpBody result = service.getFile(FileAction.File.newBuilder().setFile("index.html").build()).getReply();
    assertEquals(result, HttpBody.newBuilder().setData(
            ByteString.copyFrom(Files.readAllBytes(Paths.get("src/main/resources/web/index.html")))
            ).setContentType("text/html").build()
    );
  }

  @Test
  public void getFileNotFoundTest() throws IOException {
    FileServiceActionTestKit service = FileServiceActionTestKit.of(FileServiceAction::new);
    boolean result = service.getFile(FileAction.File.newBuilder().setFile("index_old.html").build()).isIgnore();
    assertEquals(result, true);
  }

  @Test
  public void getFileInDirTest() throws IOException {
    FileServiceActionTestKit service = FileServiceActionTestKit.of(FileServiceAction::new);
    HttpBody result = service.getFileInDir(FileAction.FileInDir.newBuilder().setDirectory("img").setFile("favicon.png").build()).getReply();
    assertEquals(result, HttpBody.newBuilder().setData(
                    ByteString.copyFrom(Files.readAllBytes(Paths.get("src/main/resources/web/img/favicon.png")))
            ).setContentType("image/png").build()
    );
  }

}
