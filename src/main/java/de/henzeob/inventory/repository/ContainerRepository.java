package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.ContainerType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContainerRepository implements PanacheRepository<Container> {

    public List<Container> findByUser(String userId) {
        return list("userId", Sort.by("name"), userId);
    }

    public List<Container> searchByNameOrDescription(String query, ContainerType type, String userId) {
        String likeQuery = "%" + query + "%";
        if (type != null) {
            return list("(LOWER(name) LIKE LOWER(?1) or LOWER(description) LIKE LOWER(?1)) and containerType = ?2 and userId = ?3",
                    Sort.by("name"), likeQuery, type, userId);
        }
        return list("(LOWER(name) LIKE LOWER(?1) or LOWER(description) LIKE LOWER(?1)) and userId = ?2",
                Sort.by("name"), likeQuery, userId);
    }

    public Optional<Container> findByIdAndUser(UUID id, String userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public List<Container> findByTypeAndUser(ContainerType type, String userId) {
        return list("containerType = ?1 and userId = ?2", Sort.by("name"), type, userId);
    }

    public List<Container> findRootContainers(String userId) {
        return list("parentContainer is null and userId = ?1", Sort.by("name"), userId);
    }

    public List<Container> findByParentAndUser(UUID parentId, String userId) {
        return list("parentContainer.id = ?1 and userId = ?2", Sort.by("name"), parentId, userId);
    }

    public void insert(Container container) {
        if (container.id == null) {
            container.id = UUID.randomUUID();
        }
        this.getEntityManager().persist(container);
    }
}
