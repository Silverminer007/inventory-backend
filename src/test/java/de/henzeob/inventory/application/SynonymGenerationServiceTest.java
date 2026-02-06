package de.henzeob.inventory.application;

import de.henzeob.inventory.model.entity.Synonym;
import de.henzeob.inventory.repository.SynonymRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SynonymGenerationServiceTest {

    @Inject
    SynonymGenerationService synonymGenerationService;

    @Inject
    SynonymRepository synonymRepository;

    @Test
    public void testGenerateSynonymsWithoutApiKey() {
        // When ANTHROPIC_API_KEY is not set, method should complete without error
        assertDoesNotThrow(() -> {
            synonymGenerationService.generateSynonyms("Laptop", "test-user");
        });
    }

    @Test
    @Transactional
    public void testSynonymPersistenceAndDeduplication() {
        String userId = "test-synonym-gen";

        // Create a synonym manually
        Synonym synonym = new Synonym();
        synonym.canonicalTerm = "laptop";
        synonym.synonym = "notebook";
        synonym.userId = userId;
        synonymRepository.persist(synonym);

        // Verify it exists
        assertTrue(synonymRepository.existsPair("laptop", "notebook", userId));

        // Verify no false positive
        assertFalse(synonymRepository.existsPair("laptop", "desktop", userId));

        // Verify different user doesn't see it
        assertFalse(synonymRepository.existsPair("laptop", "notebook", "other-user"));

        // Clean up
        List<Synonym> toDelete = synonymRepository.list("userId", userId);
        toDelete.forEach(s -> synonymRepository.delete(s));
    }
}
