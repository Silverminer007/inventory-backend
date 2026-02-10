package de.henzeob.inventory.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tag_suggestion_cache")
public class TagSuggestionCache extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "input_text", nullable = false, unique = true, columnDefinition = "TEXT")
    public String inputText;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tag_suggestion_cache_tags", joinColumns = @JoinColumn(name = "cache_id"))
    @Column(name = "tag")
    public Set<String> suggestedTags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
