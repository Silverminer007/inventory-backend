package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CategoryRepository implements PanacheRepositoryBase<Category, UUID> {

    public List<Category> findAllSorted() {
        return listAll(Sort.by("name"));
    }

    public List<Category> searchByName(String query) {
        return list("LOWER(name) LIKE LOWER(?1) OR LOWER(description) LIKE LOWER(?1)",
                Sort.by("name"), "%" + query + "%");
    }

    public Optional<Category> findByShortCode(String shortCode) {
        return find("shortCode", shortCode).firstResultOptional();
    }
}
