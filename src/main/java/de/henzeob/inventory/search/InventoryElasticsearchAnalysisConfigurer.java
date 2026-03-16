package de.henzeob.inventory.search;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import jakarta.enterprise.context.Dependent;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

@Dependent
public class InventoryElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        context.analyzer("german").custom()
                .tokenizer("standard")
                .tokenFilters("lowercase", "german_stop", "german_normalization", "german_stemmer");

        context.tokenFilter("german_stop")
                .type("stop")
                .param("stopwords", "_german_");

        context.tokenFilter("german_stemmer")
                .type("stemmer")
                .param("language", "light_german");
    }
}