package de.henzeob.inventory.application;

import de.henzeob.inventory.application.handler.ContainerCommandHandler;
import de.henzeob.inventory.application.handler.ConflictResult;
import de.henzeob.inventory.application.handler.ImageCommandHandler;
import de.henzeob.inventory.application.handler.ItemCommandHandler;
import de.henzeob.inventory.application.handler.SynonymCommandHandler;
import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandResultDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.enums.CommandStatus;
import de.henzeob.inventory.model.enums.CommandType;
import de.henzeob.inventory.repository.CommandRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CommandService {

    // Self-injection to ensure @Transactional(REQUIRES_NEW) goes through CDI proxy
    @Inject
    CommandService self;

    @Inject
    CommandRepository commandRepository;

    @Inject
    ItemCommandHandler itemCommandHandler;

    @Inject
    ContainerCommandHandler containerCommandHandler;

    @Inject
    ImageCommandHandler imageCommandHandler;

    @Inject
    SynonymCommandHandler synonymCommandHandler;

    public List<CommandResultDTO> processBatch(List<CommandDTO> commands, String userId) {
        List<CommandResultDTO> results = new ArrayList<>();
        for (CommandDTO dto : commands) {
            results.add(self.processOne(dto, userId));
        }
        return results;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public CommandResultDTO processOne(CommandDTO dto, String userId) {
        UUID commandId = dto.commandId != null ? dto.commandId : UUID.randomUUID();

        // Idempotency: if already applied, return cached result
        Optional<Command> existing = commandRepository.findByCommandIdAndUser(commandId, userId);
        if (existing.isPresent() && existing.get().status == CommandStatus.APPLIED) {
            return toResult(existing.get(), null);
        }

        // Parse and validate commandType
        CommandType commandType;
        try {
            commandType = CommandType.valueOf(dto.commandType);
        } catch (IllegalArgumentException e) {
            return failedResult(commandId, null, "Unknown commandType: " + dto.commandType);
        }

        // Validate payloadVersion
        int payloadVersion = dto.payloadVersion != null ? dto.payloadVersion : 1;
        if (payloadVersion != 1) {
            return failedResult(commandId, commandType, "Unsupported payloadVersion: " + payloadVersion);
        }

        // Validate payload presence
        if (dto.payload == null) {
            return failedResult(commandId, commandType, "Payload must not be null");
        }

        // Persist command as PENDING
        Command command = new Command();
        command.commandId = commandId;
        command.commandType = commandType;
        command.payloadVersion = payloadVersion;
        command.entityType = commandType.entityType();
        command.entityId = dto.entityId;
        command.payload = dto.payload;
        command.userId = userId;
        command.clientId = dto.clientId;
        command.clientSequence = dto.clientSequence;
        command.issuedAt = dto.issuedAt;
        command.status = CommandStatus.PENDING;
        commandRepository.persist(command);

        // Dispatch to handler — errors here must not mark the whole tx for rollback;
        // Bean Validation exceptions are thrown before SQL, so the tx stays clean.
        Object snapshot;
        try {
            snapshot = dispatch(commandType, command, userId);
        } catch (Exception e) {
            command.status = CommandStatus.FAILED;
            command.errorMessage = rootMessage(e);
            // Transaction may be marked for rollback if a DB-level exception occurred.
            // In that case this persist will also fail, but the HTTP response is still returned.
            try {
                commandRepository.persist(command);
            } catch (Exception ignored) {
                // Unable to persist failed status — acceptable; caller still gets FAILED response
            }
            CommandResultDTO result = new CommandResultDTO();
            result.commandId = commandId;
            result.status = "FAILED";
            result.entityType = command.entityType;
            result.error = rootMessage(e);
            return result;
        }

        // Handle CONFLICT — delete the pending row (no state changed) and return CONFLICT DTO
        if (snapshot instanceof ConflictResult.Conflicted conflicted) {
            commandRepository.delete(command);
            CommandResultDTO result = new CommandResultDTO();
            result.commandId = commandId;
            result.status = "CONFLICT";
            result.entityId = command.entityId;
            result.entityType = command.entityType;
            result.conflictInfo = toConflictInfo(conflicted.info());
            return result;
        }

        // Mark as APPLIED
        command.status = CommandStatus.APPLIED;
        command.appliedAt = Instant.now();
        if (snapshot != null) {
            Long entityId = extractEntityId(snapshot);
            if (entityId != null) command.entityId = entityId;
        }
        commandRepository.persist(command);

        return toResult(command, snapshot);
    }

    private Object dispatch(CommandType type, Command command, String userId) {
        String name = type.name();
        if (name.startsWith("ITEM_")) return itemCommandHandler.handle(type, command, userId);
        if (name.startsWith("CONTAINER_")) return containerCommandHandler.handle(type, command, userId);
        if (name.startsWith("IMAGE_")) return imageCommandHandler.handle(type, command, userId);
        if (name.startsWith("SYNONYM_")) return synonymCommandHandler.handle(type, command, userId);
        throw new IllegalArgumentException("No handler for command type: " + type);
    }

    private CommandResultDTO toResult(Command command, Object snapshot) {
        CommandResultDTO result = new CommandResultDTO();
        result.commandId = command.commandId;
        result.status = command.status.name();
        result.entityId = command.entityId;
        result.entityType = command.entityType;
        result.serverSequence = command.id;
        result.appliedAt = command.appliedAt;
        result.snapshot = snapshot;
        result.error = command.errorMessage;
        return result;
    }

    private CommandResultDTO failedResult(UUID commandId, CommandType commandType, String error) {
        CommandResultDTO result = new CommandResultDTO();
        result.commandId = commandId;
        result.status = "FAILED";
        result.entityType = commandType != null ? commandType.entityType() : null;
        result.error = error;
        return result;
    }

    private Long extractEntityId(Object snapshot) {
        if (snapshot == null) return null;
        try {
            var field = snapshot.getClass().getField("id");
            Object val = field.get(snapshot);
            if (val instanceof Long l) return l;
            if (val instanceof Number n) return n.longValue();
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    private CommandResultDTO.ConflictInfo toConflictInfo(ConflictResult.ConflictInfo info) {
        CommandResultDTO.ConflictInfo ci = new CommandResultDTO.ConflictInfo();
        ci.clientVersion = info.clientVersion;
        ci.serverVersion = info.serverVersion;
        ci.conflictingFields = info.conflictingFields;
        ci.serverSnapshot = info.serverSnapshot;
        ci.clientPayload = info.clientPayload;
        return ci;
    }

    private String rootMessage(Throwable t) {
        Throwable cause = t;
        int depth = 0;
        while (cause.getCause() != null && depth++ < 5) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return msg != null ? msg : t.getMessage();
    }
}
