package de.henzeob.inventory.mapper;

import de.henzeob.inventory.model.dto.SynonymDTO;
import de.henzeob.inventory.model.entity.Synonym;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SynonymMapper {

    public SynonymDTO toDTO(Synonym entity) {
        if (entity == null) return null;

        SynonymDTO dto = new SynonymDTO();
        dto.id = entity.id;
        dto.canonicalTerm = entity.canonicalTerm;
        dto.synonym = entity.synonym;
        return dto;
    }

    public Synonym toEntity(SynonymDTO dto, String userId) {
        if (dto == null) return null;

        Synonym entity = new Synonym();
        if (dto.id != null) entity.id = dto.id;
        entity.canonicalTerm = dto.canonicalTerm;
        entity.synonym = dto.synonym;
        entity.userId = userId;
        return entity;
    }
}
