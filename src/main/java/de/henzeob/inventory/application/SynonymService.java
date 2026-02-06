package de.henzeob.inventory.application;

import de.henzeob.inventory.mapper.SynonymMapper;
import de.henzeob.inventory.model.dto.SynonymDTO;
import de.henzeob.inventory.model.entity.Synonym;
import de.henzeob.inventory.repository.SynonymRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SynonymService {

    @Inject
    SynonymRepository synonymRepository;

    @Inject
    SynonymMapper synonymMapper;

    public List<SynonymDTO> getAllSynonyms(String userId) {
        return synonymRepository.findByUser(userId)
                .stream()
                .map(synonymMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SynonymDTO createSynonym(SynonymDTO dto, String userId) {
        Synonym synonym = synonymMapper.toEntity(dto, userId);
        synonymRepository.persist(synonym);
        return synonymMapper.toDTO(synonym);
    }

    @Transactional
    public void deleteSynonym(Long id, String userId) {
        Synonym synonym = synonymRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Synonym nicht gefunden"));

        // Only allow deletion of user-owned synonyms
        if (synonym.userId == null) {
            throw new IllegalArgumentException("Globale Synonyme können nicht gelöscht werden");
        }

        synonymRepository.delete(synonym);
    }

    /**
     * Expand a search query with all known synonyms (bidirectional).
     * E.g., "Handy" -> {"Handy", "Smartphone", "Mobiltelefon"}
     */
    public Set<String> expandSearchTerms(String query, String userId) {
        Set<String> terms = new HashSet<>();
        terms.add(query);

        List<Synonym> matches = synonymRepository.findSynonymsForTerm(query, userId);
        for (Synonym match : matches) {
            terms.add(match.canonicalTerm);
            terms.add(match.synonym);
        }

        return terms;
    }
}
