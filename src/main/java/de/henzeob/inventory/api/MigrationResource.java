package de.henzeob.inventory.api;

import de.henzeob.inventory.application.MigrationService;
import de.henzeob.inventory.model.dto.MigrationResultDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/migration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Migration", description = "Data migration endpoints")
public class MigrationResource {

    @Inject
    MigrationService migrationService;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @POST
    @Path("/import")
    @Operation(summary = "Import data from old inventory system",
            description = "Fetches all rooms, shelves, boxes and items from the old system and imports them")
    public Response importFromOldSystem() {
        MigrationResultDTO result = migrationService.importFromOldSystem(getCurrentUserId());
        return Response.ok(result).build();
    }
}
