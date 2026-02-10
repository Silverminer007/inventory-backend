package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.TagSuggestionCache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TagSuggestionCacheRepository implements PanacheRepository<TagSuggestionCache> {

    public Optional<TagSuggestionCache> findByInputText(String inputText) {
        return find("inputText", inputText).firstResultOptional();
    }
}
