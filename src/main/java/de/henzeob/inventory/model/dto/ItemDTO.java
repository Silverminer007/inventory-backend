package de.henzeob.inventory.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class ItemDTO {

    public UUID id;

    @NotBlank(message = "Name darf nicht leer sein")
    public String name;

    public String description;

    // Location
    public UUID containerId;
    public String position;

    @NotNull
    @Min(value = 1, message = "Menge muss mindestens 1 sein")
    public Integer quantity = 1;

    public String barcode;
    public String qrCode;
    public Set<String> tags;

    // Metadata
    public LocalDateTime lastModified;
    public Long version;
    public LocalDateTime createdAt;

    // Computed fields
    public String locationPath;
    public String containerType;

    // Nested objects (optional, for detailed views)
    public ContainerInfo container;

    public static class ContainerInfo {
        public String type;  // BOX, SHELF, ROOM
        public UUID id;
        public String name;
        public String path;
    }
}
