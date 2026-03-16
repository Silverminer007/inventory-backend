package de.henzeob.inventory.application;

import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Item;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

@ApplicationScoped
public class SearchIndexService {

    @Inject
    EntityManager entityManager;

    public void reindexAll() throws InterruptedException {
        SearchSession searchSession = Search.session(entityManager);

        searchSession.massIndexer(Item.class, Container.class)
                .batchSizeToLoadObjects(25)
                .threadsToLoadObjects(4)
                .startAndWait();
    }
}