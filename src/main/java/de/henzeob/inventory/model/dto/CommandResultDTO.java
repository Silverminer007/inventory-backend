package de.henzeob.inventory.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandResultDTO {
    public UUID commandId;
    public String status;
    public UUID entityId;
    public String entityType;
    public Long serverSequence;
    public Instant appliedAt;
    public Object snapshot;
    public String error;
    public ConflictInfo conflictInfo;

    public static class ConflictInfo {
        public Long clientVersion;
        public Long serverVersion;
        public List<String> conflictingFields;
        public Object serverSnapshot;
        public Map<String, Object> clientPayload;
    }
}
