package de.henzeob.inventory.model.dto;

import java.time.Instant;
import java.util.UUID;

public class CategoryDTO {
    public UUID id;
    public String name;
    public String description;
    public String shortCode;

    public Integer hue;
    public Long version;
    public Instant createdAt;
    public Instant lastModified;
}
