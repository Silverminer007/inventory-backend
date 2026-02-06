package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "synonyms", uniqueConstraints = @UniqueConstraint(columnNames = {"canonical_term", "synonym"}))
public class Synonym extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(name = "canonical_term", nullable = false)
    public String canonicalTerm;

    @NotBlank
    @Column(name = "synonym", nullable = false)
    public String synonym;

    @Column(name = "user_id")
    public String userId;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
