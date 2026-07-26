package de.henzeob.inventory.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "containers")
public class Container extends PanacheEntityBase {

    public static final UUID ROOT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_container_id")
    public Container parentContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public Category category;

    public String position;

    /**
     * Free-form client hint (e.g. ROOM/SHELF/BOX) - no server-side enforcement, per DOMAIN-RULES.md.
     */
    public String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_image_id")
    public Image primaryImage;

    @JsonIgnore
    @OneToMany(mappedBy = "parentContainer", cascade = CascadeType.REMOVE)
    public List<Container> childContainers = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "container", cascade = CascadeType.REMOVE)
    public List<Item> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
}
