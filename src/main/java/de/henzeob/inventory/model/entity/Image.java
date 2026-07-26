package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "images")
public class Image extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    public Container container;

    @Column(name = "s3_key", length = 500)
    public String s3Key;

    @Column(name = "content_type", length = 100)
    public String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
}
