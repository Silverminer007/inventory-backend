package de.henzeob.inventory.api;

import de.henzeob.inventory.model.dto.ContainerDTO;
import de.henzeob.inventory.application.ContainerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/containers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Containers", description = "Container management endpoints")
public class ContainerResource {

    @Inject
    ContainerService containerService;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @GET
    @Operation(summary = "Get all containers", description = "Returns all containers for the current user, optionally filtered by type or search query")
    public Response getAllContainers(
            @Parameter(description = "Filter by container type (ROOM, SHELF, BOX)") @QueryParam("type") String type,
            @Parameter(description = "Search by name or description") @QueryParam("q") String query
    ) {
        List<ContainerDTO> containers;
        if (query != null && !query.isBlank()) {
            containers = containerService.searchContainers(query,
                    type != null && !type.isBlank() ? de.henzeob.inventory.model.entity.ContainerType.valueOf(type) : null,
                    getCurrentUserId());
        } else if (type != null && !type.isBlank()) {
            containers = containerService.getContainersByType(type, getCurrentUserId());
        } else {
            containers = containerService.getAllContainers(getCurrentUserId());
        }
        return Response.ok(containers).build();
    }

    @GET
    @Path("/roots")
    @Operation(summary = "Get root containers", description = "Returns all root containers (rooms) for the current user")
    public Response getRootContainers() {
        List<ContainerDTO> roots = containerService.getRootContainers(getCurrentUserId());
        return Response.ok(roots).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get container by ID")
    public Response getContainer(
            @Parameter(description = "Container ID") @PathParam("id") Long id
    ) {
        ContainerDTO container = containerService.getContainerDTO(id, getCurrentUserId());
        return Response.ok(container).build();
    }

    @GET
    @Path("/{id}/children")
    @Operation(summary = "Get child containers")
    public Response getChildContainers(
            @Parameter(description = "Parent container ID") @PathParam("id") Long id
    ) {
        List<ContainerDTO> children = containerService.getChildContainers(id, getCurrentUserId());
        return Response.ok(children).build();
    }
}
