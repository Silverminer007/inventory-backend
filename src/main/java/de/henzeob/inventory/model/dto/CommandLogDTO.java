package de.henzeob.inventory.model.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class CommandLogDTO {
    public UUID commandId;
    public String commandType;
    public Integer payloadVersion;
    public String entityType;
    public Long entityId;
    public Map<String, Object> payload;
    public String clientId;
    public Long clientSequence;
    public Instant issuedAt;
    public Instant appliedAt;
    public Long serverSequence;
}
