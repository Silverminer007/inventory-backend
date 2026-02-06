package de.henzeob.inventory.api;

import de.henzeob.inventory.application.SynonymService;
import de.henzeob.inventory.model.dto.SynonymDTO;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/synonyms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Synonyms", description = "Synonym management endpoints")
public class SynonymResource {

    @Inject
    SynonymService synonymService;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @GET
    @Operation(summary = "Get all synonyms", description = "Returns all synonyms (global + user-specific)")
    public Response getAllSynonyms() {
        List<SynonymDTO> synonyms = synonymService.getAllSynonyms(getCurrentUserId());
        return Response.ok(synonyms).build();
    }

    @POST
    @Operation(summary = "Create synonym pair")
    public Response createSynonym(@Valid SynonymDTO dto) {
        SynonymDTO created = synonymService.createSynonym(dto, getCurrentUserId());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete synonym pair")
    public Response deleteSynonym(@PathParam("id") Long id) {
        synonymService.deleteSynonym(id, getCurrentUserId());
        return Response.noContent().build();
    }
}
