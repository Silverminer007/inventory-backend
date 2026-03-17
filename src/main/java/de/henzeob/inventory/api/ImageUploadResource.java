package de.henzeob.inventory.api;

import de.henzeob.inventory.application.ImageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

@Path("/api/v1/images/upload")
@Tag(name = "Images", description = "Image upload and management endpoints")
public class ImageUploadResource {

    @Inject
    ImageService imageService;

    private String getCurrentUserId() {
        return "demo-user";
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Pre-upload image to S3 (step 1 of two-step image upload)",
               description = "Uploads the binary file to S3 under a temporary key. Use the returned s3Key in an IMAGE_UPLOAD command to link it to an item or container.")
    public Response uploadTemp(
            @RestForm("file") FileUpload file
    ) throws IOException {
        String userId = getCurrentUserId();
        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            long fileSize = Files.size(file.uploadedFile());
            String s3Key = imageService.uploadToS3Temp(is, fileSize, file.fileName(), file.contentType(), userId);
            String s3Url = imageService.buildTempS3Url(s3Key);
            Map<String, Object> response = Map.of(
                    "s3Key", s3Key,
                    "s3Url", s3Url,
                    "filename", file.fileName() != null ? file.fileName() : "",
                    "contentType", file.contentType() != null ? file.contentType() : "",
                    "fileSize", fileSize
            );
            return Response.ok(response).build();
        }
    }
}
