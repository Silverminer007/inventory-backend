package de.henzeob.inventory.api;

import de.henzeob.inventory.application.ImageService;
import de.henzeob.inventory.model.dto.ImageDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Path("/api/v1/images")
@Tag(name = "Images", description = "Image upload and management endpoints")
public class ImageResource {

    @Inject
    ImageService imageService;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @POST
    @Path("/items/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload image for item")
    public Response uploadImageForItem(
            @PathParam("id") Long itemId,
            @RestForm("file") FileUpload file,
            @RestForm("isPrimary") @DefaultValue("false") boolean isPrimary
    ) throws IOException {
        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            ImageDTO image = imageService.uploadImageForItem(
                    itemId,
                    is,
                    Files.size(file.uploadedFile()),
                    file.fileName(),
                    file.contentType(),
                    isPrimary,
                    getCurrentUserId()
            );
            return Response.status(Response.Status.CREATED).entity(image).build();
        }
    }

    @GET
    @Path("/items/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List images for item")
    public Response getImagesForItem(@PathParam("id") Long itemId) {
        List<ImageDTO> images = imageService.getImagesForItem(itemId, getCurrentUserId());
        return Response.ok(images).build();
    }

    @POST
    @Path("/containers/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload image for container")
    public Response uploadImageForContainer(
            @PathParam("id") Long containerId,
            @RestForm("file") FileUpload file,
            @RestForm("isPrimary") @DefaultValue("false") boolean isPrimary
    ) throws IOException {
        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            ImageDTO image = imageService.uploadImageForContainer(
                    containerId,
                    is,
                    Files.size(file.uploadedFile()),
                    file.fileName(),
                    file.contentType(),
                    isPrimary,
                    getCurrentUserId()
            );
            return Response.status(Response.Status.CREATED).entity(image).build();
        }
    }

    @GET
    @Path("/containers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List images for container")
    public Response getImagesForContainer(@PathParam("id") Long containerId) {
        List<ImageDTO> images = imageService.getImagesForContainer(containerId, getCurrentUserId());
        return Response.ok(images).build();
    }

    @GET
    @Path("/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get image metadata")
    public Response getImage(@PathParam("imageId") Long imageId) {
        ImageDTO image = imageService.getImage(imageId, getCurrentUserId());
        return Response.ok(image).build();
    }

    @DELETE
    @Path("/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete image")
    public Response deleteImage(@PathParam("imageId") Long imageId) {
        imageService.deleteImage(imageId, getCurrentUserId());
        return Response.noContent().build();
    }
}
