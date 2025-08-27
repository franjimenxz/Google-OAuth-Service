package com.example.auth.controller;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/airtable")
public class AirtableController {

    private static final String AIRTABLE_URL = "https://api.airtable.com/v0/appBYN3cf0b1FqzXh/tblzLJcoNkR6nhLTV";
    private static final String TOKEN = "patcasyd7OQIPysjy.af1bfef6b5a81eaa3a8afe57875f53b7f158c441af434e22be7c3cc470d97f3f";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/create")
    public ResponseEntity<String> createRecord() {
        // --- Armamos el JSON ---
        Map<String, Object> fields = Map.of(
                "first_name", "Pepito",
                "last_name", "Postman",
                "email", "prosas@mobydigital.com",
                "technology", List.of("angular", "react", "typescript")
        );

        Map<String, Object> body = Map.of(
                "records", List.of(Map.of("fields", fields))
        );

        // --- Headers ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // --- Hacemos POST a Airtable ---
        ResponseEntity<String> response = restTemplate.postForEntity(AIRTABLE_URL, request, String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}