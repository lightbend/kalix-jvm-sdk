package com.example


import com.google.api
import com.google.api.httpbody.HttpBody
import com.google.protobuf.ByteString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.{Files, Paths}

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class FileServiceActionSpec
  extends AnyWordSpec
    with Matchers {

  "FileServiceAction" must {

    "handle command GetFile" in {
      val service = FileServiceActionTestKit(new FileServiceAction(_))
      val response: api.HttpBody = service.getFile(File("index.html")).reply

      response.data.toString("UTF-8") shouldBe ("""<!DOCTYPE html>
                                                  |<html>
                                                  |<head>
                                                  |    <link rel="icon" type="image/x-icon" href="/site/img/favicon.png">
                                                  |    <link href="/site/index.css" rel="stylesheet" />
                                                  |</head>
                                                  |<body>
                                                  |<h1>Kalix Web Resources - Demo</h1>
                                                  |<script src="/site/index.js"></script>
                                                  |<ul id="cart"></ul>
                                                  |</body>
                                                  |</html>""".stripMargin)

      response.contentType shouldBe ("text/html")
    }

    "handle command GetFileInDir" in {
      val service = FileServiceActionTestKit(new FileServiceAction(_))
      val response: api.HttpBody = service.getFileInDir(FileInDir("favicon.png", "img")).reply

      response.data shouldBe (ByteString.copyFrom(Files.readAllBytes(Paths.get("src/main/resources/web/img/favicon.png"))))
      response.contentType shouldBe ("image/png")
    }
  }
}
