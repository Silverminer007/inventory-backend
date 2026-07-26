package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Command;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CommandRepository implements PanacheRepository<Command> {

    public Optional<Command> findByCommandId(UUID commandId) {
        return find("commandId", commandId).firstResultOptional();
    }

    public Optional<Command> findRoot() {
        return find("parentCommandId is null").firstResultOptional();
    }
}
