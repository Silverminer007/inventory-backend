package de.henzeob.inventory.application;

import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TagNormalizer {

    // Beispiel-Synonym-Mapping
    private static final Map<String, String> SYNONYMS = Map.of(
            "mikro", "mikrofon",
            "bottle", "trinkflasche",
            "tasse", "becher",
            "notizbuch", "block"
            // weitere Synonyme nach Bedarf
    );

    public static String normalize(String tag) {
        if (tag == null || tag.isBlank()) return "";

        // 1. Kleinbuchstaben
        tag = tag.toLowerCase();

        // 2. Umlaute ersetzen
        tag = tag.replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");

        // 3. Sonderzeichen & Leerzeichen bereinigen (nur optional Leerzeichen behalten)
        tag = Normalizer.normalize(tag, Normalizer.Form.NFD)
                .replaceAll("[^a-z0-9 ]", "") // nur Buchstaben, Zahlen, Space
                .trim();

        // 4. Synonyme ersetzen
        tag = SYNONYMS.getOrDefault(tag, tag);

        return tag;
    }

    public static Set<String> normalizeTags(Set<String> tags) {
        return tags.stream()
                .map(TagNormalizer::normalize)
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());
    }
}