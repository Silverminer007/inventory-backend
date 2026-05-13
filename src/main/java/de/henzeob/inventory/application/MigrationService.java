package de.henzeob.inventory.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.henzeob.inventory.model.dto.BoxRenameResultDTO;
import de.henzeob.inventory.model.dto.CategoryAssignmentResultDTO;
import de.henzeob.inventory.model.dto.CategoryDTO;
import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandResultDTO;
import de.henzeob.inventory.model.dto.ContainerCategorizationResultDTO;
import de.henzeob.inventory.model.dto.ContainerDTO;
import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.model.dto.MigrationResultDTO;
import de.henzeob.inventory.model.entity.Category;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class MigrationService {

    private static final Logger LOG = Logger.getLogger(MigrationService.class);

    private static final String CLIENT_ID = "migration";
    private static final int CATEGORIZE_BATCH_SIZE = 50;

    private static final String[] ADJECTIVES = {
        "alt","arm","blass","blau","blind","breit","braun","bunt","derb","dick",
        "dicht","dumpf","dünn","dunkel","echt","eng","fein","fest","fett","flach",
        "flink","frech","frei","frisch","froh","gelb","glatt","grau","groß","grob",
        "grün","gut","hart","heiß","hell","hoch","hohl","jung","kahl","kalt","klar",
        "klein","klug","knapp","krumm","kühl","kurz","lahm","lang","laut","leer",
        "leicht","leise","lila","locker","matt","mild","mutig","nass","nett","neu",
        "offen","platt","prall","rau","reich","rein","rosa","rot","ruhig","rund",
        "satt","sanft","sauber","sauer","scharf","scheu","schief","schmal","schnell",
        "schwach","schwarz","schwer","spitz","stark","steif","steil","still","stolz",
        "straff","stumpf","süß","tapfer","tief","träge","treu","trocken","voll",
        "wach","warm","weich","weiß","weit","wild","wütend","zäh","zahm","zart",
        "blank","mürbe"
    };
    private static final String[] NOUNS = {
        "Adler","Amboss","Anker","Ast","Bach","Ball","Baum","Berg","Blatt","Blitz",
        "Boot","Brand","Brücke","Burg","Dach","Dorn","Draht","Dunst","Eis","Faden",
        "Falke","Fels","Feld","Feuer","Fisch","Fluss","Frosch","Funke","Garn",
        "Gipfel","Glas","Gold","Gras","Griff","Hahn","Hammer","Harz","Holz","Horn",
        "Hund","Hut","Igel","Kahn","Kamm","Keil","Kern","Kette","Kiefer","Klang",
        "Knoten","Korb","Kraft","Kreis","Krug","Lachs","Lamm","Laub","Licht","Luchs",
        "Mast","Mond","Moos","Nagel","Nebel","Netz","Nord","Pfad","Pfeil","Pilz",
        "Platz","Quarz","Rad","Regen","Ring","Sand","Schall","Schlag","Schloss",
        "Schnee","Schuh","Seil","Speer","Spur","Stab","Stahl","Stamm","Stein",
        "Stern","Stift","Strom","Sturm","Tal","Teer","Tier","Ton","Topf","Turm",
        "Ufer","Wald","Wand","Welle","Wind","Wolf","Wurm","Zahn","Zaun","Zelt","Zweig"
    };

    @ConfigProperty(name = "inventory.migration.old-api-base", defaultValue = "https://items.kjg-st-barbara.de/items")
    String oldApiBase;

    @ConfigProperty(name = "inventory.llm.api-key", defaultValue = "")
    ConfigValue anthropicApiKey;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CommandService commandService;

    @Inject
    ItemService itemService;

    @Inject
    CategoryService categoryService;

    @Inject
    ContainerService containerService;

    public MigrationResultDTO importFromOldSystem(String userId) {
        List<OldRoom> rooms = fetch("/Room?limit=-1", OldRoom.class);
        List<OldShelf> shelves = fetch("/Shelf?limit=-1", OldShelf.class);
        List<OldBox> boxes = fetch("/Box?limit=-1", OldBox.class);
        List<OldItem> items = fetch("/item?limit=-1", OldItem.class);

        Map<Integer, UUID> roomIdMap = new HashMap<>();
        Map<Integer, UUID> shelfIdMap = new HashMap<>();
        Map<Integer, UUID> boxIdMap = new HashMap<>();
        for (OldRoom r : rooms) {
            roomIdMap.put(r.id, UUID.randomUUID());
        }
        for (OldShelf s : shelves) {
            shelfIdMap.put(s.id, UUID.randomUUID());
        }
        for (OldBox b : boxes) {
            boxIdMap.put(b.id, UUID.randomUUID());
        }

        List<CommandDTO> commands = new ArrayList<>();
        Instant baseTime = Instant.now();
        long seq = 0;

        for (OldRoom room : rooms) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", roomIdMap.get(room.id).toString());
            payload.put("name", room.name);
            payload.put("containerType", "ROOM");
            if (room.description != null) {
                payload.put("description", room.description);
            }
            commands.add(buildCommand("CONTAINER_CREATE", payload, baseTime.plusMillis(seq++)));
        }

        int shelvesScheduled = 0;
        for (OldShelf shelf : shelves) {
            UUID parentId = roomIdMap.get(shelf.room);
            if (parentId == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", shelfIdMap.get(shelf.id).toString());
            payload.put("name", shelf.name);
            payload.put("containerType", "SHELF");
            payload.put("parentContainerId", parentId.toString());
            if (shelf.description != null) {
                payload.put("description", shelf.description);
            }
            commands.add(buildCommand("CONTAINER_CREATE", payload, baseTime.plusMillis(seq++)));
            shelvesScheduled++;
        }

        int boxesScheduled = 0;
        for (OldBox box : boxes) {
            UUID parentId = shelfIdMap.get(box.shelf);
            if (parentId == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", boxIdMap.get(box.id).toString());
            payload.put("name", box.name);
            payload.put("containerType", "BOX");
            payload.put("parentContainerId", parentId.toString());
            if (box.description != null) {
                payload.put("description", box.description);
            }
            commands.add(buildCommand("CONTAINER_CREATE", payload, baseTime.plusMillis(seq++)));
            boxesScheduled++;
        }

        int itemsScheduled = 0;
        int itemsSkipped = 0;
        for (OldItem item : items) {
            if (item.deletedAt != null) {
                itemsSkipped++;
                continue;
            }
            UUID containerId = boxIdMap.get(item.box);
            if (containerId == null) {
                itemsSkipped++;
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", item.name);
            payload.put("containerId", containerId.toString());
            payload.put("quantity", item.amount != null && item.amount >= 1 ? item.amount : 1);
            if (item.description != null) {
                payload.put("description", item.description);
            }
            commands.add(buildCommand("ITEM_CREATE", payload, baseTime.plusMillis(seq++)));
            itemsScheduled++;
        }

        List<CommandResultDTO> results = commandService.processBatch(commands, userId);

        MigrationResultDTO result = new MigrationResultDTO();
        result.roomsScheduled = rooms.size();
        result.shelvesScheduled = shelvesScheduled;
        result.boxesScheduled = boxesScheduled;
        result.itemsScheduled = itemsScheduled;
        result.itemsSkipped = itemsSkipped;
        result.commandsApplied = (int) results.stream().filter(r -> "APPLIED".equals(r.status)).count();
        result.commandsFailed = (int) results.stream().filter(r -> "FAILED".equals(r.status)).count();
        return result;
    }

    public CategoryAssignmentResultDTO categorizeItems(String userId) {
        if (anthropicApiKey == null
                || anthropicApiKey.getValue() == null
                || anthropicApiKey.getValue().isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY not set");
        }

        List<ItemDTO> allItems = itemService.getAllItems(userId);
        List<ItemDTO> toProcess = new ArrayList<>();
        int skipped = 0;
        for (ItemDTO item : allItems) {
            if (item.category != null && !Category.DEFAULT_SHORT_CODE.equals(item.category.shortCode)) {
                skipped++;
            } else {
                toProcess.add(item);
            }
        }

        List<CategoryDTO> allCategories = categoryService.getAllCategories().stream()
                .filter(c -> !Category.DEFAULT_SHORT_CODE.equals(c.shortCode))
                .toList();

        LOG.infof("categorizeItems: %d items to process, %d already categorized, %d categories available",
                toProcess.size(), skipped, allCategories.size());

        CategoryAssignmentResultDTO result = new CategoryAssignmentResultDTO();
        result.itemsSkipped = skipped;
        result.itemsProcessed = toProcess.size();

        if (allCategories.isEmpty() || toProcess.isEmpty()) {
            LOG.info("categorizeItems: nothing to do, returning early");
            return result;
        }

        Set<String> validCategoryIds = new HashSet<>();
        for (CategoryDTO c : allCategories) {
            validCategoryIds.add(c.id.toString());
        }

        List<Map<String, Object>> categoriesJson = new ArrayList<>();
        for (CategoryDTO c : allCategories) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", c.id.toString());
            entry.put("name", c.name);
            entry.put("description", c.description != null ? c.description : "");
            categoriesJson.add(entry);
        }

        List<Map<String, String>> assignments = new ArrayList<>();
        HttpClient httpClient = HttpClient.newHttpClient();
        int batchStart = 0;
        while (batchStart < toProcess.size()) {
            int batchEnd = Math.min(batchStart + CATEGORIZE_BATCH_SIZE, toProcess.size());
            List<ItemDTO> batch = toProcess.subList(batchStart, batchEnd);

            List<Map<String, Object>> itemsJson = new ArrayList<>();
            for (ItemDTO item : batch) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", item.id.toString());
                entry.put("name", item.name);
                entry.put("description", item.description != null ? item.description : "");
                itemsJson.add(entry);
            }

            int batchNumber = (batchStart / CATEGORIZE_BATCH_SIZE) + 1;
            int totalBatches = (int) Math.ceil((double) toProcess.size() / CATEGORIZE_BATCH_SIZE);
            LOG.infof("categorizeItems: sending batch %d/%d (%d items) to Claude",
                    batchNumber, totalBatches, batch.size());

            try {
                String categoriesStr = objectMapper.writeValueAsString(categoriesJson);
                String itemsStr = objectMapper.writeValueAsString(itemsJson);

                String systemPrompt = """
                        You are an inventory categorization assistant for a German youth organization.
                        Assign exactly one category to each item. Reply ONLY with a valid JSON array, no other text.
                        Format: [{"itemId":"<uuid>","categoryId":"<uuid>"}]""";

                String userMessage = "Available categories:\n" + categoriesStr
                        + "\n\nItems to categorize:\n" + itemsStr;

                Map<String, Object> payload = Map.of(
                        "model", "claude-haiku-4-5-20251001",
                        "max_tokens", 8192,
                        "system", systemPrompt,
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", List.of(
                                                Map.of("type", "text", "text", userMessage)
                                        )
                                )
                        )
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.anthropic.com/v1/messages"))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", anthropicApiKey.getValue())
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    LOG.errorf("categorizeItems: Anthropic API returned %d for batch %d/%d: %s",
                            response.statusCode(), batchNumber, totalBatches, response.body());
                    throw new IllegalStateException(
                            "Anthropic API error: " + response.statusCode() + " " + response.body());
                }

                JsonNode root = objectMapper.readTree(response.body());
                String text = root.path("content").get(0).path("text").asText("").strip();

                LOG.debugf("categorizeItems: raw response from Claude (batch %d/%d): %s", batchNumber, totalBatches, text);

                // Strip markdown code fences Claude sometimes wraps around JSON
                if (text.startsWith("```")) {
                    text = text.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```$", "").strip();
                }

                JsonNode assignmentArray = objectMapper.readTree(text);
                for (JsonNode node : assignmentArray) {
                    String itemId = node.path("itemId").asText(null);
                    String categoryId = node.path("categoryId").asText(null);
                    if (itemId == null || categoryId == null) {
                        continue;
                    }
                    if (!validCategoryIds.contains(categoryId)) {
                        LOG.warnf("categorizeItems: unrecognized categoryId %s for item %s, skipping", categoryId, itemId);
                        continue;
                    }
                    Map<String, String> entry = new HashMap<>();
                    entry.put("itemId", itemId);
                    entry.put("categoryId", categoryId);
                    assignments.add(entry);
                }
                LOG.infof("categorizeItems: batch %d/%d done, %d assignments collected so far",
                        batchNumber, totalBatches, assignments.size());
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                LOG.errorf(e, "categorizeItems: unexpected error processing batch %d/%d", batchNumber, totalBatches);
                throw new IllegalStateException("Failed to categorize batch starting at index " + batchStart, e);
            }

            batchStart = batchEnd;
        }

        Map<String, ItemDTO> itemById = new HashMap<>();
        for (ItemDTO item : toProcess) {
            itemById.put(item.id.toString(), item);
        }

        List<CommandDTO> commands = new ArrayList<>();
        Instant baseTime = Instant.now();
        long seq = 0;
        for (Map<String, String> assignment : assignments) {
            String itemId = assignment.get("itemId");
            String categoryId = assignment.get("categoryId");
            ItemDTO item = itemById.get(itemId);
            if (item == null) {
                LOG.warnf("categorizeItems: no loaded item found for id %s, skipping", itemId);
                continue;
            }
            LOG.infof("categorizeItems: mapping item '%s' (%s) to category %s", item.name, itemId, categoryId);
            Map<String, Object> cmdPayload = new HashMap<>();
            cmdPayload.put("name", item.name);
            cmdPayload.put("description", item.description);
            cmdPayload.put("position", item.position);
            cmdPayload.put("quantity", item.quantity);
            cmdPayload.put("barcode", item.barcode);
            cmdPayload.put("tags", item.tags != null ? new ArrayList<>(item.tags) : List.of());
            cmdPayload.put("category", Map.of("id", categoryId));
            cmdPayload.put("force", true);
            CommandDTO cmd = buildCommand("ITEM_UPDATE", cmdPayload, baseTime.plusMillis(seq++));
            cmd.entityId = UUID.fromString(itemId);
            commands.add(cmd);
            try {
                LOG.debugf("categorizeItems: command %s", objectMapper.writeValueAsString(cmd));
            } catch (JsonProcessingException e) {
                LOG.debugf(e, "categorizeItems: failed to serialize command %s", cmd);
            }
        }

        List<CommandResultDTO> results = commandService.processBatch(commands, userId);
        result.commandsApplied = (int) results.stream().filter(r -> "APPLIED".equals(r.status)).count();
        result.commandsFailed = (int) results.stream().filter(r -> "FAILED".equals(r.status)).count();
        results.stream().filter(r -> "FAILED".equals(r.status))
                .forEach(r -> {
                    LOG.errorf("categorizeItems: failed command: %s, %s", r.error, r.entityId);
                });
        LOG.infof("categorizeItems: finished — %d applied, %d failed (of %d assignments)",
                result.commandsApplied, result.commandsFailed, assignments.size());
        return result;
    }

    public ContainerCategorizationResultDTO propagateCategoriesToContainers(String userId) {
        ContainerCategorizationResultDTO result = new ContainerCategorizationResultDTO();

        // Load all items and group by container ID
        List<ItemDTO> allItems = itemService.getAllItems(userId);
        Map<UUID, List<ItemDTO>> itemsByContainerId = new HashMap<>();
        for (ItemDTO item : allItems) {
            if (item.containerId != null) {
                itemsByContainerId.computeIfAbsent(item.containerId, k -> new ArrayList<>()).add(item);
            }
        }

        // Phase 1: Boxes — dominant item category weighted by quantity
        List<ContainerDTO> boxes = containerService.getContainersByType("BOX", userId);
        LOG.infof("propagateCategoriesToContainers: found %d boxes", boxes.size());

        List<CommandDTO> commands = new ArrayList<>();
        Instant baseTime = Instant.now();
        long seq = 0;

        for (ContainerDTO box : boxes) {
            List<ItemDTO> boxItems = itemsByContainerId.getOrDefault(box.id, List.of());
            if (boxItems.isEmpty()) {
                result.boxesSkipped++;
                continue;
            }

            Map<UUID, Integer> categoryWeight = new HashMap<>();
            for (ItemDTO item : boxItems) {
                if (item.category != null && !Category.DEFAULT_SHORT_CODE.equals(item.category.shortCode)) {
                    categoryWeight.merge(item.category.id, item.quantity != null ? item.quantity : 1, Integer::sum);
                }
            }

            if (categoryWeight.isEmpty()) {
                result.boxesSkipped++;
                continue;
            }

            UUID dominantCategoryId = categoryWeight.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();

            if (box.primaryCategory != null && dominantCategoryId.equals(box.primaryCategory.id)) {
                result.boxesSkipped++;
                continue;
            }

            LOG.infof("propagateCategoriesToContainers: box '%s' → category %s", box.name, dominantCategoryId);
            Map<String, Object> cmdPayload = new HashMap<>();
            cmdPayload.put("name", box.name);
            cmdPayload.put("description", box.description);
            cmdPayload.put("position", box.position);
            cmdPayload.put("location", box.location);
            cmdPayload.put("primaryCategory", Map.of("id", dominantCategoryId.toString()));
            cmdPayload.put("force", true);
            CommandDTO cmd = buildCommand("CONTAINER_UPDATE", cmdPayload, baseTime.plusMillis(seq++));
            cmd.entityId = box.id;
            commands.add(cmd);
            result.boxesUpdated++;
        }

        // Phase 2: Shelves — dominant category by child-box votes (1 vote per box)
        Map<UUID, List<ContainerDTO>> boxesByParentId = new HashMap<>();
        for (ContainerDTO box : boxes) {
            if (box.parentContainerId != null) {
                boxesByParentId.computeIfAbsent(box.parentContainerId, k -> new ArrayList<>()).add(box);
            }
        }

        List<ContainerDTO> shelves = containerService.getContainersByType("SHELF", userId);
        LOG.infof("propagateCategoriesToContainers: found %d shelves", shelves.size());

        for (ContainerDTO shelf : shelves) {
            List<ContainerDTO> childBoxes = boxesByParentId.getOrDefault(shelf.id, List.of());

            Map<UUID, Integer> categoryVotes = new HashMap<>();
            for (ContainerDTO box : childBoxes) {
                if (box.primaryCategory != null && !Category.DEFAULT_SHORT_CODE.equals(box.primaryCategory.shortCode)) {
                    categoryVotes.merge(box.primaryCategory.id, 1, Integer::sum);
                }
            }

            if (categoryVotes.isEmpty()) {
                result.shelvesSkipped++;
                continue;
            }

            UUID dominantCategoryId = categoryVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();

            if (shelf.primaryCategory != null && dominantCategoryId.equals(shelf.primaryCategory.id)) {
                result.shelvesSkipped++;
                continue;
            }

            LOG.infof("propagateCategoriesToContainers: shelf '%s' → category %s", shelf.name, dominantCategoryId);
            Map<String, Object> cmdPayload = new HashMap<>();
            cmdPayload.put("name", shelf.name);
            cmdPayload.put("description", shelf.description);
            cmdPayload.put("position", shelf.position);
            cmdPayload.put("location", shelf.location);
            cmdPayload.put("primaryCategory", Map.of("id", dominantCategoryId.toString()));
            cmdPayload.put("force", true);
            CommandDTO cmd = buildCommand("CONTAINER_UPDATE", cmdPayload, baseTime.plusMillis(seq++));
            cmd.entityId = shelf.id;
            commands.add(cmd);
            result.shelvesUpdated++;
        }

        LOG.infof("propagateCategoriesToContainers: %d boxes updated, %d skipped; %d shelves updated, %d skipped; %d commands total",
                result.boxesUpdated, result.boxesSkipped, result.shelvesUpdated, result.shelvesSkipped, commands.size());

        if (commands.isEmpty()) {
            return result;
        }

        List<CommandResultDTO> results = commandService.processBatch(commands, userId);
        result.commandsApplied = (int) results.stream().filter(r -> "APPLIED".equals(r.status)).count();
        result.commandsFailed = (int) results.stream().filter(r -> "FAILED".equals(r.status)).count();
        results.stream().filter(r -> "FAILED".equals(r.status))
                .forEach(r -> LOG.errorf("propagateCategoriesToContainers: failed command: %s, %s", r.error, r.entityId));
        LOG.infof("propagateCategoriesToContainers: finished — %d applied, %d failed",
                result.commandsApplied, result.commandsFailed);
        return result;
    }

    private String generateBoxName(String shortCode) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String adj = ADJECTIVES[rnd.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[rnd.nextInt(NOUNS.length)];
        return shortCode + "-" + adj + "-" + noun;
    }

    public BoxRenameResultDTO renameBoxes(String userId) {
        BoxRenameResultDTO result = new BoxRenameResultDTO();
        List<ContainerDTO> boxes = containerService.getContainersByType("BOX", userId);
        LOG.infof("renameBoxes: found %d boxes", boxes.size());

        List<CommandDTO> commands = new ArrayList<>();
        Instant baseTime = Instant.now();
        long seq = 0;

        for (ContainerDTO box : boxes) {
            String shortCode = box.primaryCategory != null ? box.primaryCategory.shortCode : Category.DEFAULT_SHORT_CODE;
            String newName = generateBoxName(shortCode);

            BoxRenameResultDTO.RenameEntry entry = new BoxRenameResultDTO.RenameEntry();
            entry.oldName = box.name;
            entry.newName = newName;
            result.renames.add(entry);

            Map<String, Object> cmdPayload = new HashMap<>();
            cmdPayload.put("name", newName);
            cmdPayload.put("description", box.description);
            cmdPayload.put("position", box.position);
            cmdPayload.put("location", box.location);
            if (box.primaryCategory != null) {
                cmdPayload.put("primaryCategory", Map.of("id", box.primaryCategory.id.toString()));
            }
            cmdPayload.put("force", true);
            CommandDTO cmd = buildCommand("CONTAINER_UPDATE", cmdPayload, baseTime.plusMillis(seq++));
            cmd.entityId = box.id;
            commands.add(cmd);
            result.boxesRenamed++;
        }

        if (commands.isEmpty()) {
            return result;
        }

        List<CommandResultDTO> results = commandService.processBatch(commands, userId);
        result.commandsApplied = (int) results.stream().filter(r -> "APPLIED".equals(r.status)).count();
        result.commandsFailed = (int) results.stream().filter(r -> "FAILED".equals(r.status)).count();
        results.stream().filter(r -> "FAILED".equals(r.status))
                .forEach(r -> LOG.errorf("renameBoxes: failed command: %s, %s", r.error, r.entityId));
        LOG.infof("renameBoxes: finished — %d renamed, %d applied, %d failed",
                result.boxesRenamed, result.commandsApplied, result.commandsFailed);
        return result;
    }

    private CommandDTO buildCommand(String type, Map<String, Object> payload, Instant issuedAt) {
        CommandDTO cmd = new CommandDTO();
        cmd.commandId = UUID.randomUUID();
        cmd.commandType = type;
        cmd.payload = payload;
        cmd.issuedAt = issuedAt;
        cmd.clientId = CLIENT_ID;
        return cmd;
    }

    private <T> List<T> fetch(String path, Class<T> elementType) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(oldApiBase + path))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.get("data");
            return objectMapper.convertValue(data,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch from old API: " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching from old API: " + path, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OldRoom {
        public Integer id;
        public String name;
        public String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OldShelf {
        public Integer id;
        public String name;
        public String description;
        public Integer room;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OldBox {
        public Integer id;
        public String name;
        public String description;
        public Integer shelf;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OldItem {
        public Integer id;
        public String name;
        public String description;
        public Integer amount;
        public Integer box;

        @JsonProperty("deleted_at")
        public String deletedAt;
    }
}
