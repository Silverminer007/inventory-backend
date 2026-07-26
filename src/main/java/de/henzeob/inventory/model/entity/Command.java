package de.henzeob.inventory.model.entity;

import de.henzeob.inventory.model.enums.CommandType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "commands")
public class Command extends PanacheEntityBase {

    public static final UUID ROOT_COMMAND_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "command_id", unique = true, nullable = false)
    public UUID commandId;

    @Column(name = "parent_command_id", unique = true)
    public UUID parentCommandId;

    @Column(name = "child_command_id", unique = true)
    public UUID childCommandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false)
    public CommandType commandType;

    @Column(name = "command_version", nullable = false)
    public Integer commandVersion = 1;

    @Column(name = "entity_id")
    public UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> payload;

    @Column(name = "created_at", updatable = false, nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "applied_at")
    public Instant appliedAt;
}
