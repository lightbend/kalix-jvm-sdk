package com.example.json

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

@JsonCreator
case class JsonKeyValueMessage(
  @JsonProperty("key") key: String,
  @JsonProperty("value") value: Int)
