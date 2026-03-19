package de.henzeob.inventory.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.henzeob.inventory.model.entity.Synonym;
import de.henzeob.inventory.repository.SynonymRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SynonymGenerationService {

    @Inject
    SynonymRepository synonymRepository;

    /**
     * Generate synonyms for an item name using LLM and persist them.
     * Graceful fallback: catches all exceptions so item creation is never affected.
     */
    public Set<String> generateSynonyms(String itemName, String userId) {
        List<Synonym> cachedSynonyms = synonymRepository.findSynonymsForTerm(itemName, userId);
        if(!cachedSynonyms.isEmpty()) {
            return cachedSynonyms.stream().map(synonym -> synonym.synonym).collect(Collectors.toSet());
        }
        Set<String> generatedSynonyms = new HashSet<>();
        try {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                return new HashSet<>();
            }

            List<String> synonymTerms = callLlm(itemName, apiKey);

            for (String term : synonymTerms) {
                if (term.equalsIgnoreCase(itemName)) {
                    continue;
                }
                if (!synonymRepository.existsPair(itemName.toLowerCase(), term, userId)) {
                    Synonym synonym = new Synonym();
                    synonym.canonicalTerm = itemName.toLowerCase();
                    synonym.synonym = term;
                    synonym.userId = userId;
                    synonymRepository.persist(synonym);
                    generatedSynonyms.add(synonym.synonym);
                }
            }
        } catch (Exception e) {
            System.err.println("Synonym generation failed: " + e.getMessage());
        }
        return generatedSynonyms;
    }

    private List<String> callLlm(String itemName, String apiKey) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String prompt = """
                Du bist ein Inventar-Assistent. Für den folgenden Gegenstand, gib mir alternative \
                Bezeichnungen und Suchbegriffe, unter denen man diesen Gegenstand auch finden könnte.
                
                Gib nur eine kommagetrennte Liste zurück. Keine Sätze, keine Erklärungen.
                Alles klein geschrieben. Maximal 5 Begriffe.
                
                Gegenstand: "%s"
                """.formatted(itemName);

        Map<String, Object> payload = Map.of(
                "model", "claude-3-haiku-20240307",
                "max_tokens", 100,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", prompt
                                        )
                                )
                        )
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(payload)
                ))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Anthropic API error: " + response.statusCode() + " " + response.body()
            );
        }

        JsonNode root = mapper.readTree(response.body());

        String text = root
                .path("content")
                .get(0)
                .path("text")
                .asText("");

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
