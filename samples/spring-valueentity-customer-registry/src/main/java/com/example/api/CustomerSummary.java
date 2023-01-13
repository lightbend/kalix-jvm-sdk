package com.example.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.time.Instant;

// tag::record[]
public record CustomerSummary(String id, String name, double createdAt) { }
// end::record[]
