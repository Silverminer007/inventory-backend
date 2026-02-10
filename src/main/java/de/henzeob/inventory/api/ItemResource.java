package de.henzeob.inventory.api;

import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.application.ItemService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Items", description = "Item management endpoints")
public class ItemResource {

    @Inject
    ItemService itemService;

    // TODO: Später mit Keycloak Security Context ersetzen
    // Für MVP: Hardcoded User
    private String getCurrentUserId() {
        return "demo-user";
    }

    @GET
    @Operation(summary = "Get all items", description = "Returns all items for the current user")
    public Response getAllItems() {
        List<ItemDTO> items = itemService.getAllItems(getCurrentUserId());
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get item by ID")
    public Response getItem(
            @Parameter(description = "Item ID") @PathParam("id") Long id
    ) {
        ItemDTO item = itemService.getItem(id, getCurrentUserId());
        return Response.ok(item).build();
    }

    @POST
    @Operation(summary = "Create new item")
    public Response createItem(@Valid ItemDTO dto) {
        ItemDTO created = itemService.createItem(dto, getCurrentUserId());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update item")
    public Response updateItem(
            @PathParam("id") Long id,
            @Valid ItemDTO dto
    ) {
        ItemDTO updated = itemService.updateItem(id, dto, getCurrentUserId());
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete item")
    public Response deleteItem(@PathParam("id") Long id) {
        itemService.deleteItem(id, getCurrentUserId());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/move")
    @Operation(summary = "Move item to different container")
    public Response moveItem(
            @PathParam("id") Long id,
            MoveRequest request
    ) {
        ItemDTO moved = itemService.moveItem(
                id,
                getCurrentUserId(),
                request.containerId
        );
        return Response.ok(moved).build();
    }

    @GET
    @Path("/tags")
    @Operation(summary = "Get distinct tags", description = "Returns all distinct tags for the current user, optionally filtered by prefix")
    public Response getDistinctTags(
            @QueryParam("q") @DefaultValue("") String prefix
    ) {
        List<String> tags = itemService.getDistinctTags(getCurrentUserId(), prefix);
        return Response.ok(tags).build();
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search items by name")
    public Response searchItems(
            @QueryParam("q") @DefaultValue("") String query
    ) {
        List<ItemDTO> results = itemService.searchItems(query, getCurrentUserId());
        return Response.ok(results).build();
    }

    @GET
    @Path("/by-tag/{tag}")
    @Operation(summary = "Get items by tag")
    public Response getItemsByTag(@PathParam("tag") String tag) {
        List<ItemDTO> items = itemService.getItemsByTag(tag, getCurrentUserId());
        return Response.ok(items).build();
    }

    // DTO for move request
    public static class MoveRequest {
        public Long containerId;
    }
}
