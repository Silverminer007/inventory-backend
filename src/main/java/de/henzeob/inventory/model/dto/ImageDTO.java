package de.henzeob.inventory.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ImageDTO {

    public UUID id;
    public UUID itemId;
    public UUID containerId;
    public String filename;
    public String contentType;
    public Long fileSize;
    public boolean isPrimary;
    public LocalDateTime uploadedAt;
    public String url;
}
