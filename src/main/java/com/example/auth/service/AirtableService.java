package com.example.auth.service;

import com.example.auth.model.AirtableCreateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
@Service
public class AirtableService {

    private final WebClient client;
    private final String baseId;
    private final String tableIdOrName;

    public AirtableService(
            WebClient airtableClient,
            @Value("${airtable.baseId}") String baseId,
            @Value("${airtable.tableIdOrName}") String tableIdOrName
    ) {
        this.client = airtableClient;
        this.baseId = baseId;
        this.tableIdOrName = tableIdOrName;
    }

    public Mono<String> createPepito() {
        Map<String, Object> fields = Map.of(
                "first_name", "Pepito",
                "last_name", "Postman",
                "email", "prosas@mobydigital.com",
                "technology", List.of("angular", "react", "typescript") // multi-select o array
        );

        var body = AirtableCreateRequest.ofSingle(fields);

        return client.post()
                .uri("/{base}/{table}", baseId, tableIdOrName)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class); // o un DTO de respuesta si querés
    }


    public Mono<Map<String, Object>> getAllRecords() {
        return client.get()
                .uri(uriBuilder -> uriBuilder.pathSegment(baseId, tableIdOrName).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    // 🔹 GET por ID específico
    public Mono<String> getRecordById(String recordId) {
        return client.get()
                .uri("/{base}/{table}/{id}", baseId, tableIdOrName, recordId)
                .retrieve()
                .bodyToMono(String.class);
    }
}