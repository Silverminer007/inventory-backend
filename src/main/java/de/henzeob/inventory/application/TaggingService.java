package de.henzeob.inventory.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.henzeob.inventory.model.entity.ItemTag;
import de.henzeob.inventory.model.entity.TagSuggestionCache;
import de.henzeob.inventory.repository.TagSuggestionCacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaggingService {
    @ConfigProperty(name = "inventory.llm.api-key", defaultValue = "")
    ConfigValue anthropicApiKey;

    @ConfigProperty(name = "inventory.llm.tagging")
    Boolean useLLMTagging;

    @Inject
    TagSuggestionCacheRepository tagSuggestionCacheRepository;

    // Rule-based Tagging mit Keywords
    private static final Map<String, List<String>> KEYWORD_RULES = Map.ofEntries(
            Map.entry("Technik", List.of(
                    "laptop", "computer", "handy", "smartphone", "tablet", "kabel",
                    "ladegerät", "powerbank", "usb", "akku", "batterie", "kopfhörer",
                    "headset", "maus", "tastatur", "monitor", "bildschirm", "beamer",
                    "netzteil", "stecker", "cinch", "klinke", "box", "bass", "mischpult",
                    "speakon", "sattelite", "top", "steckdose", "xlr", "batterien",
                    "beamer", "drucker", "tv", "fernseher", "fernbedienung", "bluetooth", "jbl",
                    "funk", "mikro", "walkie", "talkie", "laser"
            )),
            Map.entry("Outdoor", List.of(
                    "zelt", "schlafsack", "isomatte", "rucksack", "wanderschuhe",
                    "taschenlampe", "kompass", "messer", "feuerzeug", "streichhölzer",
                    "plane", "seil", "karabiner", "campingstuhl", "feldbett", "gaskocher"
            )),
            Map.entry("Basteln", List.of(
                    "schere", "kleber", "papier", "karton", "farb", "pinsel",
                    "stift", "marker", "buntstift", "wolle", "nadel", "faden",
                    "bastelkleber", "glitzer", "perlen", "knöpfe", "pfeifenreiniger",
                    "anspitzer", "radierer", "radiergummi", "malkasten", "deckweiß",
                    "creppapier", "edding", "stempel", "klebeband", "stoff", "wolle",
                    "filz", "krepp", "crepp", "tesa"
            )),
            Map.entry("Spiele", List.of(
                    "ball", "frisbee", "würfel", "karten", "brettspiel", "puzzle",
                    "spielzeug", "puppe", "figur", "lego", "playmobil", "kartenspiel",
                    "domino", "jojo", "drachen", "hüpfburg"
            )),
            Map.entry("Küche", List.of(
                    "teller", "tasse", "besteck", "gabel", "messer", "löffel",
                    "topf", "pfanne", "schneidebrett", "kelle", "korkenzieher",
                    "flaschenöffner", "dose", "becher", "thermoskanne", "kühlbox",
                    "alufolie", "frischhaltefolie", "warmhalte", "schevy", "geschirr", "tuch", "tücher"
            )),
            Map.entry("Erste-Hilfe", List.of(
                    "pflaster", "verband", "schere", "pinzette", "desinfektionsmittel",
                    "salbe", "schmerzmittel", "fieberthermometer", "wundauflage",
                    "rettungsdecke", "dreiecktuch", "medikamente"
            )),
            Map.entry("Werkzeug", List.of(
                    "hammer", "schraubendreher", "schraubenzieher",
                    "zange", "säge", "bohrmaschine",
                    "akkuschrauber", "bit", "bitsatz",
                    "schraube", "nagel", "dübel",
                    "schraubenschlüssel", "werkzeugkoffer"
            )),
            Map.entry("Sport", List.of(
                    "fußball", "volleyball", "basketball", "seil", "hütchen",
                    "leibchen", "pfeife", "stoppuhr", "turnmatte", "springschnur",
                    "badminton", "ball", "bälle", "feld", "tischtennis", "wikingerschach"
            )),
            Map.entry("Bücher", List.of(
                    "buch", "comic", "ltb", "seil", "lexikon", "chronik", "edition",
                    "donald", "duck", "isbn", "roman", "heft"
            )),
            Map.entry("Putzmittel", List.of(
                    "reiniger", "putzmittel", "allzweckreiniger", "spülmittel",
                    "glasreiniger", "badreiniger", "wc-reiniger", "entkalker",
                    "scheuermilch", "desinfektionsmittel", "bodenreiniger",
                    "putztuch", "lappen", "schwamm", "bürste", "besen",
                    "kehrschaufel", "wischmopp", "eimer", "seife"
            )),
            Map.entry("Gesellschaftsspiele", List.of(
                    // generisch
                    "brettspiel", "kartenspiel", "würfelspiel", "gesellschaftsspiel",
                    "familienpiel", "partyspiel", "denkspiel", "ratespiel",
                    "quizspiel", "kooperativ", "strategie", "taktik",
                    "spielbrett", "spielfiguren", "spielkarten", "würfel",
                    "spielanleitung", "spielbox", "quiz",

                    // marken / titel
                    "catan", "monopoly", "risiko", "carcassonne", "siedler",
                    "scrabble", "mensch ärgere dich nicht", "uno", "phase 10",
                    "kniffel", "cluedo", "tabu", "activity", "memory",
                    "skat", "doppelkopf", "rommé", "maumau", "poker", "wizard",
                    "werwolf", "werwölfe"
            )),
            Map.entry("Getränke", List.of(
                    "wasser", "mineralwasser", "sprudel", "still",
                    "saft", "apfelsaft", "orangensaft", "multivitaminsaft",
                    "limo", "limonade", "cola", "eistee",
                    "tee", "kaffee", "kakao",
                    "milch", "hafermilch",
                    "bier", "wein", "sekt",
                    "energy", "energydrink", "isodrink",
                    "sirup", "schorle", "punsch"
            )),
            Map.entry("KjG", List.of(
                    "kjg", "katholische junge gemeinde", "kifrei", "kinderfreizeit"
            )),
            Map.entry("Luna", List.of(
                    "luna", "grusel", "puppe"
            )),
            Map.entry("Musik", List.of(
                    "instrument", "gitarre", "e-gitarre", "bass", "ukulele",
                    "klavier", "keyboard", "schlagzeug", "cajon",
                    "mikrofon", "verstärker", "lautsprecher", "box",
                    "kabel", "noten", "notenständer", "metronom"
            )),
            Map.entry("Verkleidung", List.of(
                    "kostüm", "verkleidung", "maske", "perücke",
                    "hut", "cape", "umhang", "brille",
                    "schminke", "theatermaske", "accessoire"
            )),
            Map.entry("Kleidung", List.of(
                    "hose", "t-shirt", "shirt", "pullover", "jacke",
                    "mantel", "kleid", "rock", "socken",
                    "schuhe", "turnschuhe", "sandalen",
                    "mütze", "schal", "handschuhe"
            )),
            Map.entry("Deko", List.of(
                    "dekoration", "deko", "girlande", "luftballon",
                    "kerze", "lichterkette", "plakat", "banner",
                    "figur", "aufsteller", "tischdeko"
            )),
            Map.entry("Bürobedarf", List.of(
                    "papier", "block", "heft", "ordner",
                    "stift", "kugelschreiber", "bleistift", "marker",
                    "textmarker", "radiergummi", "lineal",
                    "locher", "hefter", "tacker", "klammern"
            )),
            Map.entry("Sommer", List.of(
                    "sonnencreme", "sonnenschutz", "sonnenbrille",
                    "hut", "kappe", "fächer",
                    "trinkflasche", "kühlbox",
                    "picknickdecke", "ventilator"
            )),
            Map.entry("Schwimmen/Baden/Wasserschlacht", List.of(
                    "badehose", "badeanzug", "bikini",
                    "handtuch", "badetuch",
                    "schwimmflügel", "schwimmring",
                    "wasserpistole", "wasserballon",
                    "taucherbrille", "schnorchel",
                    "wasserpistole", "wasserbomben",
                    "pool", "wassereimer", "eimer"
            )),
            Map.entry("Strand", List.of(
                    "strandtuch", "strandmatte", "sonnenschirm",
                    "strandstuhl", "strandtasche",
                    "sandspielzeug", "eimer", "schaufel",
                    "muscheln", "strandball"
            )),
            Map.entry("SingStar (Playstation)", List.of(
                    "singstar", "playstation", "ps2", "ps3", "ps4",
                    "mikrofon", "usb-mikrofon",
                    "spiel", "dvd", "konsole"
            )),
            Map.entry("Zelten", List.of(
                    "zelt", "schlafsack", "isomatte",
                    "luftmatratze", "campingstuhl",
                    "campingtisch", "lampe", "stirnlampe",
                    "gaskocher", "kartusche",
                    "hering", "zeltstange"
            )),
            Map.entry("Backen/Kochen", List.of(
                    "schüssel", "rührschüssel", "löffel",
                    "schneebesen", "teigschaber",
                    "backform", "kuchenform",
                    "messbecher", "waage",
                    "nudelholz", "backpapier",
                    "kochtopf", "pfanne"
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
    public Set<ItemTag> generateTags(String itemName, String description) {
        Set<ItemTag> tags = new HashSet<>();

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
                    ItemTag itemTag = new ItemTag();
                    itemTag.setTag(tag);
                    itemTag.setTagType(ItemTag.TagType.RULES);
                    tags.add(itemTag);
                    break; // Nur einmal pro Tag
                }
            }
        }

        // 2. Regex-basierte Tags
        for (var entry : REGEX_RULES.entrySet()) {
            String tag = entry.getKey();
            Pattern pattern = entry.getValue();

            if (pattern.matcher(fullText).find()) {
                ItemTag itemTag = new ItemTag();
                itemTag.setTag(tag);
                itemTag.setTagType(ItemTag.TagType.RULES);
                tags.add(itemTag);
            }
        }

        // 3. Spezielle Regeln
        tags.addAll(applySpecialRules(fullText));

        if (tags.size() < 3) {
            tags.addAll(suggestTags(fullText));
        }

        return tags;
    }

    /**
     * Spezielle Tagging-Regeln
     */
    private Set<ItemTag> applySpecialRules(String text) {
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

        return tags.stream().map(tag ->  {
            ItemTag itemTag = new ItemTag();
            itemTag.setTag(tag);
            itemTag.setTagType(ItemTag.TagType.RULES);
            return itemTag;
        }).collect(Collectors.toSet());
    }

    /**
     * Suggest tags based on similar items (für später mit ML)
     */
    public Set<ItemTag> suggestTags(String itemName) {
        try {
            if (useLLMTagging == null || !useLLMTagging) {
                return new HashSet<>();
            }

            // Check cache first
            Optional<TagSuggestionCache> cached = tagSuggestionCacheRepository.findByInputText(itemName);
            if (cached.isPresent()) {
                return cached.get().suggestedTags.stream().map(tag ->  {
                    ItemTag itemTag = new ItemTag();
                    itemTag.setTag(tag);
                    itemTag.setTagType(ItemTag.TagType.LLM);
                    return itemTag;
                }).collect(Collectors.toSet());
            }

            if (this.anthropicApiKey == null
                    || this.anthropicApiKey.getValue() == null
                    || this.anthropicApiKey.getValue().isBlank()) {
                throw new IllegalStateException("ANTHROPIC_API_KEY not set");
            }

            ObjectMapper mapper = new ObjectMapper();

            String prompt = """
                    Gib mir eine kommagetrennte Liste kurzer, allgemeiner Tags
                    (passend für Kategorien, Gegenstände).
                    Keine Sätze, keine Erklärungen.
                    Alles klein geschrieben.
                    Maximal 3 Tags

                    Gegenstand: "%s"
                    """.formatted(itemName);

            Map<String, Object> payload = Map.of(
                    "model", "claude-3-haiku-20240307",
                    "max_tokens", 100,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "text",
                                                    "text", prompt
                                            )
                                    )
                            )
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", this.anthropicApiKey.getValue())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(payload)
                    ))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Anthropic API error: " + response.statusCode() + " " + response.body()
                );
            }

            JsonNode root = mapper.readTree(response.body());

            String text = root
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText("");

            Set<String> tags = Arrays.stream(text.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());
            Set<String> normalizedTags = TagNormalizer.normalizeTags(tags);

            // Store in cache
            TagSuggestionCache cacheEntry = new TagSuggestionCache();
            cacheEntry.inputText = itemName;
            cacheEntry.suggestedTags = normalizedTags;
            tagSuggestionCacheRepository.persist(cacheEntry);

            return normalizedTags.stream().map(tag ->  {
                ItemTag itemTag = new ItemTag();
                itemTag.setTag(tag);
                itemTag.setTagType(ItemTag.TagType.LLM);
                return itemTag;
            }).collect(Collectors.toSet());

        } catch (Exception e) {
            // Fallback: kein ML, keine Tags
            System.err.println("Tag suggestion failed: " + e.getMessage());
            return Set.of();
        }
    }
}