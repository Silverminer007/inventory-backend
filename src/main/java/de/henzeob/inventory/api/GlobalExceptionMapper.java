package de.henzeob.inventory.api;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("message", exception.getMessage());

        Response.Status status;

        if (exception instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            errorResponse.put("message", "Ein Fehler ist aufgetreten");
        }

        errorResponse.put("status", status.getStatusCode());
        errorResponse.put("error", status.getReasonPhrase());

        return Response
                .status(status)
                .entity(errorResponse)
                .build();
    }
}