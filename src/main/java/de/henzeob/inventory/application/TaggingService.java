package de.henzeob.inventory.application;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class TaggingService {

    // Rule-based Tagging mit Keywords
    private static final Map<String, List<String>> KEYWORD_RULES = Map.ofEntries(
            Map.entry("Technik", List.of(
                    "laptop", "computer", "handy", "smartphone", "tablet", "kabel",
                    "ladegerät", "powerbank", "usb", "akku", "batterie", "kopfhörer",
                    "headset", "maus", "tastatur", "monitor", "bildschirm", "beamer"
            )),
            Map.entry("Outdoor", List.of(
                    "zelt", "schlafsack", "isomatte", "rucksack", "wanderschuhe",
                    "taschenlampe", "kompass", "messer", "feuerzeug", "streichhölzer",
                    "plane", "seil", "karabiner", "campingstuhl", "feldbett", "gaskocher"
            )),
            Map.entry("Basteln", List.of(
                    "schere", "kleber", "papier", "karton", "farbe", "pinsel",
                    "stift", "marker", "buntstift", "wolle", "nadel", "faden",
                    "bastelkleber", "glitzer", "perlen", "knöpfe", "pfeifenreiniger"
            )),
            Map.entry("Spiele", List.of(
                    "ball", "frisbee", "würfel", "karten", "brettspiel", "puzzle",
                    "spielzeug", "puppe", "figur", "lego", "playmobil", "kartenspiel",
                    "domino", "jojo", "drachen", "hüpfburg"
            )),
            Map.entry("Küche", List.of(
                    "teller", "tasse", "besteck", "gabel", "messer", "löffel",
                    "topf", "pfanne", "schneidebrett", "kelle", "korkenzieher",
                    "flaschenöffner", "dose", "becher", "thermoskanne", "kühlbox"
            )),
            Map.entry("Erste-Hilfe", List.of(
                    "pflaster", "verband", "schere", "pinzette", "desinfektionsmittel",
                    "salbe", "schmerzmittel", "fieberthermometer", "wundauflage",
                    "rettungsdecke", "dreiecktuch"
            )),
            Map.entry("Werkzeug", List.of(
                    "hammer", "schraubendreher", "schraubenzieher", "zange", "säge",
                    "bohrmaschine", "akkuschrauber", "wasserwaage", "maßband",
                    "zollstock", "schraube", "nagel", "dübel", "schraubenschlüssel"
            )),
            Map.entry("Sport", List.of(
                    "fußball", "volleyball", "basketball", "seil", "hütchen",
                    "leibchen", "pfeife", "stoppuhr", "turnmatte", "springschnur"
            ))
    );

    // Regex-basierte Regeln
    private static final Map<String, Pattern> REGEX_RULES = Map.of(
            "Elektrik", Pattern.compile(".*?\\d+\\s*[vV].*|.*?\\d+\\s*mAh.*|.*?[vV]olt.*",
                    Pattern.CASE_INSENSITIVE),
            "Messbar", Pattern.compile(".*?\\d+\\s*(cm|mm|m|kg|g|l|ml).*",
                    Pattern.CASE_INSENSITIVE)
    );

    /**
     * Generate tags for an item based on name and description
     */
    public Set<String> generateTags(String itemName, String description) {
        Set<String> tags = new HashSet<>();

        String fullText = (itemName + " " + (description != null ? description : ""))
                .toLowerCase()
                .trim();

        if (fullText.isBlank()) {
            return tags;
        }

        // 1. Keyword-basierte Tags
        for (var entry : KEYWORD_RULES.entrySet()) {
            String tag = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (fullText.contains(keyword.toLowerCase())) {
                    tags.add(tag);
                    break; // Nur einmal pro Tag
                }
            }
        }

        // 2. Regex-basierte Tags
        for (var entry : REGEX_RULES.entrySet()) {
            String tag = entry.getKey();
            Pattern pattern = entry.getValue();

            if (pattern.matcher(fullText).find()) {
                tags.add(tag);
            }
        }

        // 3. Spezielle Regeln
        tags.addAll(applySpecialRules(fullText));

        return tags;
    }

    /**
     * Spezielle Tagging-Regeln
     */
    private Set<String> applySpecialRules(String text) {
        Set<String> tags = new HashSet<>();

        // Größen
        if (text.contains("klein")) tags.add("Klein");
        if (text.contains("groß") || text.contains("gross")) tags.add("Groß");

        // Material
        if (text.contains("holz")) tags.add("Holz");
        if (text.contains("metall") || text.contains("eisen") || text.contains("stahl")) {
            tags.add("Metall");
        }
        if (text.contains("kunststoff") || text.contains("plastik")) {
            tags.add("Kunststoff");
        }
        if (text.contains("glas")) tags.add("Glas");

        // Farben
        List<String> colors = List.of(
                "rot", "blau", "grün", "gelb", "schwarz",
                "weiß", "weiss", "orange", "lila", "pink", "rosa"
        );

        for (String color : colors) {
            if (text.contains(color)) {
                String capitalizedColor = color.substring(0, 1).toUpperCase() + color.substring(1);
                tags.add("Farbe: " + capitalizedColor);
                break; // Nur eine Farbe
            }
        }

        // Zustand
        if (text.contains("neu") && !text.contains("erneu")) tags.add("Neu");
        if (text.contains("alt")) tags.add("Gebraucht");
        if (text.contains("defekt") || text.contains("kaputt")) tags.add("Defekt");

        // Anzahl
        if (text.matches(".*\\d+\\s*x\\s*.*") || text.matches(".*\\d+\\s*stück.*")) {
            tags.add("Menge");
        }

        return tags;
    }

    /**
     * Suggest tags based on similar items (für später mit ML)
     */
    public Set<String> suggestTags(String itemName) {
        // TODO: Implementiere ML-basiertes Tagging
        return new HashSet<>();
    }
}