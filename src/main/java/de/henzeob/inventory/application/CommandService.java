package de.henzeob.inventory.application;

import de.henzeob.inventory.application.handler.CategoryCommandHandler;
import de.henzeob.inventory.application.handler.ContainerCommandHandler;
import de.henzeob.inventory.application.handler.ContainerImageCommandHandler;
import de.henzeob.inventory.application.handler.ItemCommandHandler;
import de.henzeob.inventory.application.handler.ItemImageCommandHandler;
import de.henzeob.inventory.exceptions.InvalidCommandPayloadException;
import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.enums.CommandType;
import de.henzeob.inventory.repository.CommandRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CommandService {

    @Inject
    CommandRepository commandRepository;

    @Inject
    ItemCommandHandler itemCommandHandler;

    @Inject
    ContainerCommandHandler containerCommandHandler;

    @Inject
    CategoryCommandHandler categoryCommandHandler;

    @Inject
    ItemImageCommandHandler itemImageCommandHandler;

    @Inject
    ContainerImageCommandHandler containerImageCommandHandler;

    @Inject
    Clock clock;

    public Command applyCommand(CommandDTO dto) {
        if (dto.id == null) {
            throw new InvalidCommandPayloadException();
        }
        CommandType commandType = parseCommandType(dto.commandType);
        if (dto.commandVersion == null || dto.commandVersion != 1) {
            throw new InvalidCommandPayloadException();
        }
        if (dto.payload == null) {
            throw new InvalidCommandPayloadException();
        }

        try {
            dispatch(commandType, dto.payload);
        } catch (InvalidCommandPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCommandPayloadException();
        }

        Command command = new Command();
        command.commandId = dto.id;
        command.commandType = commandType;
        command.commandVersion = 1;
        command.entityId = extractEntityId(dto.payload);
        command.payload = dto.payload;
        command.appliedAt = Instant.now(clock);

        commandRepository.persist(command);
        return command;
    }

    private CommandType parseCommandType(String value) {
        if (value == null) {
            throw new InvalidCommandPayloadException();
        }
        try {
            return CommandType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandPayloadException();
        }
    }

    private void dispatch(CommandType type, Map<String, Object> payload) {
        String name = type.name();
        if (name.startsWith("ITEM_IMAGE_")) {
            itemImageCommandHandler.handle(type, payload);
        } else if (name.startsWith("CONTAINER_IMAGE_")) {
            containerImageCommandHandler.handle(type, payload);
        } else if (name.startsWith("ITEM_")) {
            itemCommandHandler.handle(type, payload);
        } else if (name.startsWith("CONTAINER_")) {
            containerCommandHandler.handle(type, payload);
        } else if (name.startsWith("CATEGORY_")) {
            categoryCommandHandler.handle(type, payload);
        } else {
            throw new InvalidCommandPayloadException();
        }
    }

    private UUID extractEntityId(Map<String, Object> payload) {
        Object id = payload.get("id");
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
