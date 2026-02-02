package de.henzeob.inventory.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "containers")
public class Container extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "container_type", nullable = false)
    public ContainerType containerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_container_id")
    public Container parentContainer;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type")
    public LocationType locationType;

    public String location;

    public String position;

    @Column(name = "qr_code", unique = true)
    public String qrCode;

    @JsonIgnore
    @OneToMany(mappedBy = "parentContainer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    public List<Container> childContainers = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "container", cascade = CascadeType.REMOVE, orphanRemoval = true)
    public List<Item> items = new ArrayList<>();

    @NotNull
    @Column(name = "last_modified", nullable = false)
    public LocalDateTime lastModified = LocalDateTime.now();

    @Version
    @NotNull
    public Long version;

    @NotBlank
    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Returns the location path by walking up the parent chain.
     * E.g. "Keller > Regal A > Box 1"
     */
    public String getLocationPath() {
        if (parentContainer == null) {
            return "";
        }
        String parentPath = parentContainer.getFullPath();
        return parentPath;
    }

    /**
     * Returns the full path including this container's name.
     */
    public String getFullPath() {
        if (parentContainer == null) {
            return name;
        }
        String parentPath = parentContainer.getFullPath();
        return parentPath + " > " + name;
    }

    /**
     * Recursive total item count including all descendants.
     */
    public int getTotalItemCount() {
        int count = items.size();
        for (Container child : childContainers) {
            count += child.getTotalItemCount();
        }
        return count;
    }

    public boolean isTemporary() {
        return locationType == LocationType.TEMPORARY;
    }

    public enum LocationType {
        PERMANENT,
        TEMPORARY
    }
}
