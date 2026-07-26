package de.henzeob.inventory.api;

import de.henzeob.inventory.application.ImageCompressor;
import de.henzeob.inventory.application.ImageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;

@Path("/api/v2")
@Tag(name = "Images", description = "Direct image upload/download endpoints, proxying to S3")
public class ImageUploadResource {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");

    @Inject
    ImageService imageService;

    @Inject
    ImageCompressor imageCompressor;

    @POST
    @Path("/uploadImage/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload the binary data for a previously created ITEM_IMAGE/CONTAINER_IMAGE")
    public Response uploadImage(@PathParam("id") UUID id, @RestForm("file") FileUpload file) throws IOException {
        if (file == null || file.contentType() == null || !SUPPORTED_CONTENT_TYPES.contains(file.contentType().toLowerCase())) {
            throw new BadRequestException("Only PNG and JPEG images are supported");
        }
        long fileSize = Files.size(file.uploadedFile());
        if (fileSize > MAX_UPLOAD_BYTES) {
            throw new BadRequestException("Image exceeds maximum upload size of 5MB");
        }

        byte[] rawData = Files.readAllBytes(file.uploadedFile());
        byte[] compressed = imageCompressor.compressToJpeg(rawData);
        imageService.storeBinary(id, compressed, "image/jpeg");

        return Response.ok().build();
    }

    @GET
    @Path("/images/{id}")
    public Response downloadImage(@PathParam("id") UUID id) {
        ResponseBytes<GetObjectResponse> object;
        try {
            object = imageService.downloadBinary(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException();
        }
        String contentType = object.response().contentType() != null ? object.response().contentType() : "application/octet-stream";
        return Response.ok(object.asByteArray()).type(contentType).build();
    }
}
