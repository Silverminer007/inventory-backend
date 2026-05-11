package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.enums.CommandStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CommandRepository implements PanacheRepository<Command> {

    public Optional<Command> findByCommandIdAndUser(UUID commandId, String userId) {
        return find("commandId = ?1 and userId = ?2", commandId, userId).firstResultOptional();
    }

    public List<Command> findByUserAppliedSince(String userId, Instant since, int limit) {
        return find("(userId = ?1 or userId = 'system') and status = ?2 and appliedAt >= ?3",
                Sort.by("appliedAt"),
                userId, CommandStatus.APPLIED, since)
                .page(0, limit)
                .list();
    }

    /**
     * Returns the {@code limit} most recently applied commands for a given entity,
     * ordered by insertion id descending. Used for 3-way merge field-conflict detection.
     */
    public List<Command> findRecentApplied(UUID entityId, String entityType, int limit) {
        if (limit <= 0) return List.of();
        return find("entityId = ?1 and entityType = ?2 and status = ?3",
                Sort.by("id").descending(),
                entityId, entityType, CommandStatus.APPLIED)
                .page(0, limit)
                .list();
    }
}
