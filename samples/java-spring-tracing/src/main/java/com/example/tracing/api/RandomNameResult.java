package com.example.tracing.api;

import java.util.List;

public record RandomNameResult(List<Result> results) {
  public record Result(ResultName name) { }
  public record ResultName(String title, String first, String last) {}

  public String name() {
    // We only expect one result
    return results.get(0).name().first() + " " + results.get(0).name().last();
  }
}
