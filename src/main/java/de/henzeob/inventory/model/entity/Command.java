package de.henzeob.inventory.model.entity;

import de.henzeob.inventory.model.enums.CommandStatus;
import de.henzeob.inventory.model.enums.CommandType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "commands")
public class Command extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "command_id", unique = true, nullable = false)
    public UUID commandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false)
    public CommandType commandType;

    @Column(name = "payload_version", nullable = false)
    public Integer payloadVersion = 1;

    @Column(name = "entity_type", nullable = false)
    public String entityType;

    @Column(name = "entity_id")
    public Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> payload;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "client_id")
    public String clientId;

    @Column(name = "client_sequence")
    public Long clientSequence;

    @Column(name = "issued_at")
    public Instant issuedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "applied_at")
    public Instant appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CommandStatus status = CommandStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
}
