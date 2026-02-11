package de.henzeob.inventory.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "item_tags")
public class ItemTag extends PanacheEntity {
    @Column(name = "tag")
    private String tag;
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type")
    private TagType tagType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    @JsonIgnore
    private Item item;

    public enum TagType {
        USER, LLM, RULES;
    }
}
