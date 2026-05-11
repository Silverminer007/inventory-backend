package de.henzeob.inventory.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandResultDTO;
import de.henzeob.inventory.model.dto.MigrationResultDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class MigrationService {

    private static final String OLD_API_BASE = "https://items.kjg-st-barbara.de/items";
    private static final String CLIENT_ID = "migration";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CommandService commandService;

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
                    .uri(URI.create(OLD_API_BASE + path))
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
