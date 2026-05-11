package de.henzeob.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class CategoryDTO {
    public UUID id;
    @NotBlank(message = "Name darf nicht leer sein")
    public String name;
    public String description;
    @NotBlank(message = "Kürzel darf nicht leer und muss einmalig sein")
    public String shortCode;

    public Integer hue;
    public Long version;
    public Instant createdAt = Instant.now();
    public Instant lastModified = Instant.now();
}
