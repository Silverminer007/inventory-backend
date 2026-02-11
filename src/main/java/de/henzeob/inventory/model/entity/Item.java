package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "items")
public class Item extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    public Container container;

    public String position;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    public Integer quantity = 1;

    public String barcode;

    @Column(name = "qr_code", unique = true)
    public String qrCode;

    @NotNull
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "item")
    public Set<ItemTag> tags = new HashSet<>();

    @NotNull
    @Column(name = "last_modified", nullable = false)
    public LocalDateTime lastModified = LocalDateTime.now();

    @Version
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

    public String getLocationPath() {
        StringBuilder path = new StringBuilder();

        if (container != null) {
            path.append(container.getFullPath());
        }

        if (position != null && !position.isBlank()) {
            if (path.length() > 0) path.append(" > ");
            path.append(position);
        }

        return path.toString();
    }

    public String getContainerType() {
        return container != null ? container.containerType.name() : "UNKNOWN";
    }

    public Long getContainerId() {
        return container != null ? container.id : null;
    }
}
