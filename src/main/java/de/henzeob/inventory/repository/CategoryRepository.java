package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class CategoryRepository implements PanacheRepositoryBase<Category, UUID> {
}
