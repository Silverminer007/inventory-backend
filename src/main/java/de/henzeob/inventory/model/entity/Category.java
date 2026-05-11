package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String name;
    public String description;
    @Column(unique = true, name = "short_code", nullable = false)
    public String shortCode;

    @Column(nullable = false)
    public Integer hue;

    @Version
    @NotNull
    public Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();
    @NotNull
    @Column(name = "last_modified", nullable = false)
    public Instant lastModified = Instant.now();
}
