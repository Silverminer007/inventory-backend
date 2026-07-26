package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "short_code", nullable = false)
    public String shortCode;

    public Integer hue;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
}
