package de.henzeob.inventory.application;

import de.henzeob.inventory.repository.SynonymRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class OpenThesaurusImporter {

    private static final Logger LOG = Logger.getLogger(OpenThesaurusImporter.class);
    private static final int BATCH_SIZE = 500;
    private static final int LOG_INTERVAL = 10000;
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("\\s*\\([^)]*\\)\\s*");

    @ConfigProperty(name = "inventory.openthesaurus.import.enabled", defaultValue = "true")
    boolean importEnabled;

    @Inject
    SynonymRepository synonymRepository;

    @Inject
    EntityManager entityManager;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!importEnabled) {
            LOG.info("OpenThesaurus import disabled by configuration");
            return;
        }

        long globalCount = synonymRepository.countGlobal();
        if (globalCount > 50) {
            LOG.infof("OpenThesaurus synonyms already loaded (%d entries), skipping", globalCount);
            return;
        }

        LOG.info("Loading OpenThesaurus synonyms...");
        importSynonyms();
    }

    void importSynonyms() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("openthesaurus.txt")) {

            if (is == null) {
                LOG.warn("openthesaurus.txt not found on classpath, skipping import");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            int lineCount = 0;
            int pairCount = 0;
            int batchCount = 0;

            QuarkusTransaction.commit();
            QuarkusTransaction.begin();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                List<String> terms = parseSynsetLine(line);
                if (terms.size() < 2) {
                    continue;
                }

                String canonical = terms.get(0);

                for (int i = 1; i < terms.size(); i++) {
                    String synonym = terms.get(i);
                    entityManager.createNativeQuery(
                            "INSERT INTO synonyms (canonical_term, synonym, user_id, created_at) " +
                            "VALUES (:canonical, :synonym, NULL, NOW()) ON CONFLICT DO NOTHING")
                            .setParameter("canonical", canonical)
                            .setParameter("synonym", synonym)
                            .executeUpdate();
                    batchCount++;
                    pairCount++;

                    if (batchCount >= BATCH_SIZE) {
                        entityManager.flush();
                        entityManager.clear();
                        QuarkusTransaction.commit();
                        QuarkusTransaction.begin();
                        batchCount = 0;
                    }
                }

                // Cross-pairs between non-canonical terms
                for (int i = 1; i < terms.size(); i++) {
                    for (int j = i + 1; j < terms.size(); j++) {
                        entityManager.createNativeQuery(
                                "INSERT INTO synonyms (canonical_term, synonym, user_id, created_at) " +
                                "VALUES (:canonical, :synonym, NULL, NOW()) ON CONFLICT DO NOTHING")
                                .setParameter("canonical", terms.get(i))
                                .setParameter("synonym", terms.get(j))
                                .executeUpdate();
                        batchCount++;
                        pairCount++;

                        if (batchCount >= BATCH_SIZE) {
                            entityManager.flush();
                            entityManager.clear();
                            QuarkusTransaction.commit();
                            QuarkusTransaction.begin();
                            batchCount = 0;
                        }
                    }
                }

                lineCount++;
                if (lineCount % LOG_INTERVAL == 0) {
                    LOG.infof("Processed %d synsets, %d pairs so far...", lineCount, pairCount);
                }
            }

            entityManager.flush();
            entityManager.clear();
            QuarkusTransaction.commit();

            LOG.infof("OpenThesaurus import complete: %d synsets, %d synonym pairs", lineCount, pairCount);

        } catch (Exception e) {
            LOG.error("Failed to import OpenThesaurus data", e);
            try {
                QuarkusTransaction.rollback();
            } catch (Exception rollbackEx) {
                LOG.debug("Rollback after import failure", rollbackEx);
            }
        }
    }

    private List<String> parseSynsetLine(String line) {
        String[] parts = line.split(";");
        List<String> terms = new ArrayList<>();

        for (String part : parts) {
            String cleaned = ANNOTATION_PATTERN.matcher(part).replaceAll("").trim();
            if (!cleaned.isEmpty() && !cleaned.contains("...")) {
                terms.add(cleaned.toLowerCase());
            }
        }

        return terms;
    }
}
