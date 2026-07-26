package de.henzeob.inventory.model.dto;

import java.util.UUID;

/**
 * A single entry in the command linked list as transmitted over the wire.
 * parent/child are the bookkeeping fields of the doubly-linked list; command is the actual command.
 */
public class CommandEntryDTO {
    public UUID parent;
    public UUID child;
    public CommandDTO command;

    public CommandEntryDTO() {
    }

    public CommandEntryDTO(UUID parent, UUID child, CommandDTO command) {
        this.parent = parent;
        this.child = child;
        this.command = command;
    }
}
