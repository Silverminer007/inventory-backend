package de.henzeob.inventory.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class AuditLogService {

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logCreate(String userId, String entityType, Long entityId,
                          String entityName, Object entity) {
        log(userId, "CREATE", entityType, entityId, entityName, null, entity,
                entityName + " erstellt");
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logUpdate(String userId, String entityType, Long entityId,
                          String entityName, Object oldValue, Object newValue) {
        log(userId, "UPDATE", entityType, entityId, entityName, oldValue, newValue,
                entityName + " aktualisiert");
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logDelete(String userId, String entityType, Long entityId,
                          String entityName, Object entity) {
        log(userId, "DELETE", entityType, entityId, entityName, entity, null,
                entityName + " gelöscht");
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logMove(String userId, String entityType, Long entityId,
                        String entityName, String oldLocation, String newLocation) {
        log(userId, "MOVE", entityType, entityId, entityName,
                Map.of("location", oldLocation),
                Map.of("location", newLocation),
                entityName + " verschoben von '" + oldLocation + "' nach '" + newLocation + "'");
    }

    private void log(String userId, String action, String entityType, Long entityId,
                     String entityName, Object oldValue, Object newValue, String description) {
        try {
            String oldJson = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newJson = newValue != null ? objectMapper.writeValueAsString(newValue) : null;

            em.createNativeQuery("""
                INSERT INTO audit_log 
                (user_id, timestamp, action, entity_type, entity_id, entity_name, 
                 old_value, new_value, description)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7::jsonb, ?8::jsonb, ?9)
            """)
                    .setParameter(1, userId)
                    .setParameter(2, LocalDateTime.now())
                    .setParameter(3, action)
                    .setParameter(4, entityType)
                    .setParameter(5, entityId)
                    .setParameter(6, entityName)
                    .setParameter(7, oldJson)
                    .setParameter(8, newJson)
                    .setParameter(9, description)
                    .executeUpdate();
        } catch (JsonProcessingException e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }

    private static class Map {
        public static java.util.Map<String, String> of(String key, String value) {
            return java.util.Collections.singletonMap(key, value);
        }
    }
}