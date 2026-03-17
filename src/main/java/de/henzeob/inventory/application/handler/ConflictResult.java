package de.henzeob.inventory.application.handler;

import java.util.List;
import java.util.Map;

public sealed interface ConflictResult {
    record Applied(Object snapshot) implements ConflictResult {}
    record Conflicted(ConflictInfo info) implements ConflictResult {}

    class ConflictInfo {
        public Long clientVersion;
        public Long serverVersion;
        public List<String> conflictingFields;
        public Object serverSnapshot;
        public Map<String, Object> clientPayload;
    }
}
