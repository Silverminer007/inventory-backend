package de.henzeob.inventory.model.dto;

import java.time.LocalDateTime;

public class ImageDTO {

    public Long id;
    public Long itemId;
    public Long containerId;
    public String filename;
    public String contentType;
    public Long fileSize;
    public boolean isPrimary;
    public LocalDateTime uploadedAt;
    public String url;
}
