package de.henzeob.inventory.api;

import de.henzeob.inventory.application.CategoryService;
import de.henzeob.inventory.model.dto.CategoryDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    @GET
    @Operation(summary = "Get all categories", description = "Returns all categories, optionally filtered by name search")
    public Response getAllCategories(
            @Parameter(description = "Search by name or description") @QueryParam("q") String query
    ) {
        List<CategoryDTO> categories = query != null && !query.isBlank()
                ? categoryService.searchByName(query)
                : categoryService.getAllCategories();
        return Response.ok(categories).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get category by ID")
    public Response getCategory(
            @Parameter(description = "Category ID") @PathParam("id") UUID id
    ) {
        return Response.ok(categoryService.getCategory(id)).build();
    }

    @GET
    @Path("/by-short-code/{shortCode}")
    @Operation(summary = "Get category by short code")
    public Response getCategoryByShortCode(
            @Parameter(description = "Category short code") @PathParam("shortCode") String shortCode
    ) {
        return Response.ok(categoryService.getCategoryByShortCode(shortCode)).build();
    }
}