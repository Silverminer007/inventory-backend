package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.ImageService;
import de.henzeob.inventory.model.dto.ImageDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.enums.CommandType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ImageCommandHandler {

    @Inject
    ImageService imageService;

    public Object handle(CommandType type, Command command, String userId) {
        Map<String, Object> p = command.payload;
        return switch (type) {
            case IMAGE_UPLOAD      -> handleUpload(p, userId);
            case IMAGE_DELETE      -> {
                handleDelete(command.entityId, userId);
                yield null;
            }
            case IMAGE_SET_PRIMARY -> handleSetPrimary(command.entityId, userId);
            default -> throw new IllegalArgumentException("Not an IMAGE command: " + type);
        };
    }

    private ImageDTO handleUpload(Map<String, Object> p, String userId) {
        UUID id = toUUID(p.get("id"));
        String s3Key = required(p, "s3Key");
        String filename = (String) p.get("filename");
        String contentType = (String) p.get("contentType");
        Long fileSize = toLong(p.get("fileSize"));
        boolean isPrimary = p.get("isPrimary") instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(p.get("isPrimary")));
        UUID itemId = toUUID(p.get("itemId"));
        UUID containerId = toUUID(p.get("containerId"));
        return imageService.linkImageFromS3Key(id, s3Key, filename, contentType, fileSize, isPrimary, itemId, containerId, userId);
    }

    private void handleDelete(UUID entityId, String userId) {
        imageService.deleteImage(entityId, userId);
    }

    private ImageDTO handleSetPrimary(UUID entityId, String userId) {
        return imageService.setPrimaryImage(entityId, userId);
    }

    @SuppressWarnings("unchecked")
    private <T> T required(Map<String, Object> p, String key) {
        Object val = p.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required payload field: " + key);
        return (T) val;
    }

    private UUID toUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
}
