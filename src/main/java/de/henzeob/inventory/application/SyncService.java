package de.henzeob.inventory.application;

import de.henzeob.inventory.exceptions.InvalidCommandPayloadException;
import de.henzeob.inventory.exceptions.InvalidCommandReferenceException;
import de.henzeob.inventory.exceptions.InvalidHeadException;
import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandEntryDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.repository.CommandRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SyncService {

    @Inject
    CommandRepository commandRepository;

    @Inject
    CommandService commandService;

    public List<CommandEntryDTO> fetchCommands(String since) {
        Command start;
        if (since == null) {
            start = commandRepository.findRoot().orElseThrow(() -> new IllegalStateException("Root command missing"));
        } else {
            UUID sinceId = parseUuidBadRequest(since);
            Command sinceCommand = commandRepository.findByCommandId(sinceId)
                    .orElseThrow(NotFoundException::new);
            start = loadChild(sinceCommand);
        }
        return loadChain(start);
    }

    @Transactional
    public String applyCommands(String head, List<CommandEntryDTO> entries) {
        if (head == null) {
            throw new InvalidCommandReferenceException();
        }
        UUID headId = parseUuidBadRequest(head);
        Command headCommand = commandRepository.findByCommandId(headId)
                .orElseThrow(InvalidCommandReferenceException::new);

        if (headCommand.childCommandId != null) {
            throw new InvalidHeadException(findTip(headCommand).commandId.toString());
        }

        List<Command> applied = applyCommandChain(headCommand, entries);

        Command newHead = headCommand;
        if (!applied.isEmpty()) {
            headCommand.childCommandId = applied.get(0).commandId;
            newHead = applied.get(applied.size() - 1);
        }
        return newHead.commandId.toString();
    }

    private List<Command> applyCommandChain(Command head, List<CommandEntryDTO> entries) {
        Map<UUID, CommandEntryDTO> byParent = new HashMap<>();
        for (CommandEntryDTO entry : entries) {
            if (entry == null || entry.command == null || entry.parent == null) {
                throw new InvalidCommandPayloadException();
            }
            if (byParent.put(entry.parent, entry) != null) {
                throw new InvalidCommandPayloadException();
            }
        }

        // The supplied head is the real tip, but no command in the batch attaches to it:
        // the client built its chain on a different base -> conflict, not a malformed payload.
        if (!entries.isEmpty() && !byParent.containsKey(head.commandId)) {
            throw new InvalidHeadException(head.commandId.toString());
        }

        List<Command> applied = new ArrayList<>();
        UUID nextParent = head.commandId;
        Command previous = null;
        CommandEntryDTO next;
        while ((next = byParent.get(nextParent)) != null) {
            Command command = commandService.applyCommand(next.command);
            command.parentCommandId = nextParent;
            if (previous != null) {
                previous.childCommandId = command.commandId;
            }
            applied.add(command);
            previous = command;
            nextParent = command.commandId;
        }

        if (applied.size() != entries.size()) {
            throw new InvalidCommandPayloadException();
        }
        return applied;
    }

    private Command findTip(Command from) {
        Command current = from;
        Command next;
        while ((next = loadChild(current)) != null) {
            current = next;
        }
        return current;
    }

    private Command loadChild(Command command) {
        if (command == null || command.childCommandId == null) {
            return null;
        }
        return commandRepository.findByCommandId(command.childCommandId)
                .orElseThrow(() -> new IllegalStateException("Dangling child_command_id: " + command.childCommandId));
    }

    private List<CommandEntryDTO> loadChain(Command start) {
        List<CommandEntryDTO> result = new ArrayList<>();
        Command current = start;
        while (current != null) {
            result.add(toEntryDto(current));
            current = loadChild(current);
        }
        return result;
    }

    private CommandEntryDTO toEntryDto(Command command) {
        CommandDTO dto = new CommandDTO();
        dto.id = command.commandId;
        dto.commandType = command.commandType.name();
        dto.commandVersion = command.commandVersion;
        dto.payload = command.payload;
        return new CommandEntryDTO(command.parentCommandId, command.childCommandId, dto);
    }

    private UUID parseUuidBadRequest(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandReferenceException();
        }
    }
}
