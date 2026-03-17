package de.henzeob.inventory.api;

import de.henzeob.inventory.application.ImageService;
import de.henzeob.inventory.model.dto.ImageDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/images")
@Tag(name = "Images", description = "Image upload and management endpoints")
public class ImageResource {

    @Inject
    ImageService imageService;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @GET
    @Path("/items/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List images for item")
    public Response getImagesForItem(@PathParam("id") UUID itemId) {
        List<ImageDTO> images = imageService.getImagesForItem(itemId, getCurrentUserId());
        return Response.ok(images).build();
    }

    @GET
    @Path("/containers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List images for container")
    public Response getImagesForContainer(@PathParam("id") UUID containerId) {
        List<ImageDTO> images = imageService.getImagesForContainer(containerId, getCurrentUserId());
        return Response.ok(images).build();
    }

    @GET
    @Path("/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get image metadata")
    public Response getImage(@PathParam("imageId") UUID imageId) {
        ImageDTO image = imageService.getImage(imageId, getCurrentUserId());
        return Response.ok(image).build();
    }
}
