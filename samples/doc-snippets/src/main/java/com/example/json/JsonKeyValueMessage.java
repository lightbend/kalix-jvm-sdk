package com.example.json;

public class JsonKeyValueMessage {
  public final String key;
  public final int value;

 public JsonKeyValueMessage(String key, int value) {
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
