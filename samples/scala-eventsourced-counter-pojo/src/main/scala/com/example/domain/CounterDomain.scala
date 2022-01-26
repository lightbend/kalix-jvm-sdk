package com.example.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeName


case class CounterState @JsonCreator()(value: Int)
case class ValueIncreased @JsonCreator()(value: Int)
case class ValueDecreased @JsonCreator()(value: Int)

trait ValueReset
@JsonTypeName("reset")
object ValueReset extends ValueReset
