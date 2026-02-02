package de.henzeob.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class ContainerDTO {

    public Long id;

    @NotBlank(message = "Name darf nicht leer sein")
    public String name;

    public String description;

    @NotNull(message = "Container-Typ ist erforderlich")
    public String containerType;  // ROOM, SHELF, BOX

    public String locationType;   // PERMANENT, TEMPORARY (only for ROOM)
    public String location;       // only for ROOM

    public String position;
    public String qrCode;

    public Long parentContainerId;

    // Metadata
    public LocalDateTime lastModified;
    public Long version;
    public LocalDateTime createdAt;

    // Computed fields
    public String locationPath;
    public int itemCount;
    public int totalItemCount;

    // Optional nested data
    public List<ContainerDTO> children;
    public ParentInfo parent;

    public static class ParentInfo {
        public Long id;
        public String name;
        public String containerType;
    }
}
