package de.henzeob.inventory.api;

import de.henzeob.inventory.application.SyncService;
import de.henzeob.inventory.model.dto.CommandEntryDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import java.util.List;

@Path("/api/v2/sync")
public class SyncResource {

    @Inject
    SyncService syncService;

    @GET
    @Path("/fetchCommands")
    public Response fetchCommands(@QueryParam("since") String since) {
        List<CommandEntryDTO> commands = syncService.fetchCommands(since);
        return Response.ok(commands).build();
    }

    @POST
    @Path("/applyCommands")
    public Response applyCommands(@QueryParam("head") String head, @RequestBody List<CommandEntryDTO> commands) {
        String newHead = syncService.applyCommands(head, commands);
        return Response.ok(newHead).build();
    }
}
