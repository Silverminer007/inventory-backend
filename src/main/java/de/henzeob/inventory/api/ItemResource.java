package de.henzeob.inventory.api;

import de.henzeob.inventory.application.ImageService;
import de.henzeob.inventory.application.SynonymGenerationService;
import de.henzeob.inventory.application.TaggingService;
import de.henzeob.inventory.model.dto.ImageDTO;
import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.application.ItemService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Set;
import java.util.UUID;


@Path("/api/v1/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Items", description = "Item management endpoints")
public class ItemResource {

    @Inject
    ItemService itemService;
    @Inject
    TaggingService taggingService;
    @Inject
    SynonymGenerationService synonymGenerationService;
    @Inject
    ImageService imageService;

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
            @Parameter(description = "Item ID") @PathParam("id") UUID id
    ) {
        ItemDTO item = itemService.getItem(id, getCurrentUserId());
        return Response.ok(item).build();
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
    @Path("/tags/suggest")
    public Response suggestTags(@QueryParam("item") String item) {
        Set<String> tags = this.taggingService.suggestTags(item);
        return Response.ok(tags).build();
    }

    @GET
    @Path("/synonyms/suggest")
    public Response suggestSynonyms(@QueryParam("item") String item) {
        Set<String> tags = this.synonymGenerationService.generateSynonyms(item, getCurrentUserId());
        return Response.ok(tags).build();
    }

    @GET
    @Path("/by-tag/{tag}")
    @Operation(summary = "Get items by tag")
    public Response getItemsByTag(@PathParam("tag") String tag) {
        List<ItemDTO> items = itemService.getItemsByTag(tag, getCurrentUserId());
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}/images")
    @Operation(summary = "Get images for item")
    public Response getImagesForItem(@PathParam("id") UUID id) {
        List<ImageDTO> images = imageService.getImagesForItem(id, getCurrentUserId());
        return Response.ok(images).build();
    }

    @POST
    @Path("/{id}/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload image for item")
    public Response uploadImageForItem(
            @PathParam("id") UUID id,
            @RestForm("file") FileUpload file,
            @RestForm("primary") @DefaultValue("false") boolean isPrimary
    ) throws IOException {
        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            long fileSize = Files.size(file.uploadedFile());
            ImageDTO image = imageService.uploadImageForItem(
                    id, is, fileSize, file.fileName(), file.contentType(), isPrimary, getCurrentUserId());
            return Response.status(Response.Status.CREATED).entity(image).build();
        }
    }
}
