package de.henzeob.inventory.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

/**
 * Wire shape of a single command, per DOMAIN-RULES.md: only id/command_type/command_version/payload
 * are part of the command itself. Chain position (parent/child) lives on {@link CommandEntryDTO}.
 */
public class CommandDTO {
    public UUID id;

    @JsonProperty("command_type")
    public String commandType;

    @JsonProperty("command_version")
    public Integer commandVersion;

    public Map<String, Object> payload;
}
