package com.example.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonKeyValueMessage {
  public final String key;
  public final int value;

 @JsonCreator
 public JsonKeyValueMessage(@JsonProperty("key") String key, @JsonProperty("value") int value) {
  this.key = key;
  this.value = value;
 }

 @Override
 public String toString() {
  return "JsonKeyValueMessage{" +
      "key='" + key + '\'' +
      ", value=" + value +
      '}';
 }
}
