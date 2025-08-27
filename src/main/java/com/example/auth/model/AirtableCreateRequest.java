package com.example.auth.model;


import java.util.List;
import java.util.Map;

public record AirtableCreateRequest(List<Record> records) {
    public static AirtableCreateRequest ofSingle(Map<String, Object> fields) {
        return new AirtableCreateRequest(List.of(new Record(fields)));
    }
    public record Record(Map<String, Object> fields) {}
}