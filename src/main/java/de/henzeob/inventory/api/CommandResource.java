package de.henzeob.inventory.api;

import de.henzeob.inventory.application.CommandService;
import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandLogDTO;
import de.henzeob.inventory.model.dto.CommandResultDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.repository.CommandRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/v1/commands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Commands", description = "Event-sourcing command API")
public class CommandResource {

    @Inject
    CommandService commandService;

    @Inject
    CommandRepository commandRepository;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @POST
    @Operation(summary = "Submit a batch of commands",
               description = "Process one or more commands. Each command is applied atomically. Returns a result per command.")
    public Response processBatch(List<CommandDTO> commands) {
        if (commands == null || commands.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Command batch must not be empty\"}")
                    .build();
        }
        List<CommandResultDTO> results = commandService.processBatch(commands, getCurrentUserId());
        return Response.ok(results).build();
    }

    @GET
    @Operation(summary = "Sync: fetch applied commands since a timestamp",
               description = "Returns all APPLIED commands for the current user at or after the given ISO-8601 timestamp.")
    public Response getCommandsSince(
            @QueryParam("since") String since,
            @QueryParam("limit") @DefaultValue("500") int limit
    ) {
        if (since == null || since.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Query parameter 'since' is required\"}")
                    .build();
        }
        Instant sinceInstant;
        try {
            sinceInstant = Instant.parse(since);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid 'since' timestamp: " + since + "\"}")
                    .build();
        }
        int effectiveLimit = Math.min(limit, 1000);
        List<Command> commands = commandRepository.findByUserAppliedSince(getCurrentUserId(), sinceInstant, effectiveLimit);
        List<CommandLogDTO> result = commands.stream().map(this::toLogDTO).collect(Collectors.toList());
        return Response.ok(result).build();
    }

    private CommandLogDTO toLogDTO(Command command) {
        CommandLogDTO dto = new CommandLogDTO();
        dto.commandId = command.commandId;
        dto.commandType = command.commandType.name();
        dto.payloadVersion = command.payloadVersion;
        dto.entityType = command.entityType;
        dto.entityId = command.entityId;
        dto.payload = command.payload;
        dto.clientId = command.clientId;
        dto.clientSequence = command.clientSequence;
        dto.issuedAt = command.issuedAt;
        dto.appliedAt = command.appliedAt;
        dto.serverSequence = command.id;
        return dto;
    }
}
