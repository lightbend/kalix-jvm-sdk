package com.example.tracing.api;

import java.util.List;

public interface RandomUserApi {
  record Name(List<Result> results) {
    public record Result(ResultName name) {
    }

    public record ResultName(String title, String first, String last) {
    }

    public String name() {
      // We always expect at least and only one result
      return results.get(0).name().first() + " " + results.get(0).name().last();
    }
  }

  record Photo(List<Result> results) {
    public record Result(ResultPicture picture) { }
    public record ResultPicture(String large) {}

    public String url() {
      // We only expect one result
      return results.get(0).picture.large;
    }
  }


}
