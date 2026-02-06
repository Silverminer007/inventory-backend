package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos")
public class Image extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    public Container container;

    @NotBlank
    @Column(name = "s3_key", nullable = false, length = 500)
    public String s3Key;

    @Column(name = "s3_url", length = 1000)
    public String s3Url;

    @Column(length = 255)
    public String filename;

    @Column(name = "content_type", length = 100)
    public String contentType;

    @Column(name = "file_size")
    public Long fileSize;

    @Column(name = "is_primary", nullable = false)
    public boolean isPrimary = false;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    public LocalDateTime uploadedAt = LocalDateTime.now();

    @NotBlank
    @Column(name = "user_id", nullable = false)
    public String userId;
}
