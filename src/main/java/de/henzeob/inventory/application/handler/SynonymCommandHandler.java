package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.SynonymService;
import de.henzeob.inventory.model.dto.SynonymDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.enums.CommandType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SynonymCommandHandler {

    @Inject
    SynonymService synonymService;

    public Object handle(CommandType type, Command command, String userId) {
        Map<String, Object> p = command.payload;
        return switch (type) {
            case SYNONYM_CREATE -> handleCreate(p, userId);
            case SYNONYM_DELETE -> { handleDelete(command.entityId, userId); yield null; }
            default -> throw new IllegalArgumentException("Not a SYNONYM command: " + type);
        };
    }

    private SynonymDTO handleCreate(Map<String, Object> p, String userId) {
        SynonymDTO dto = new SynonymDTO();
        dto.canonicalTerm = required(p, "canonicalTerm");
        dto.synonym = required(p, "synonym");
        return synonymService.createSynonym(dto, userId);
    }

    private void handleDelete(UUID entityId, String userId) {
        synonymService.deleteSynonym(entityId, userId);
    }

    @SuppressWarnings("unchecked")
    private <T> T required(Map<String, Object> p, String key) {
        Object val = p.get(key);
        if (val == null || (val instanceof String s && s.isBlank()))
            throw new IllegalArgumentException("Missing required payload field: " + key);
        return (T) val;
    }
}
