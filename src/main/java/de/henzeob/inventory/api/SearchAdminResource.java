package de.henzeob.inventory.api;

import de.henzeob.inventory.application.SearchIndexService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/admin/search")
//@RolesAllowed("ADMIN_API")
public class SearchAdminResource {

    @Inject
    SearchIndexService searchIndexService;

    @POST
    @Path("/reindex")
    public Response reindex() throws InterruptedException {
        searchIndexService.reindexAll();
        return Response.ok("Reindex abgeschlossen").build();
    }
}