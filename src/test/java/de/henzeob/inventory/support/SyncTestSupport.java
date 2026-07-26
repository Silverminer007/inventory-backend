package de.henzeob.inventory.support;

import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandEntryDTO;
import de.henzeob.inventory.model.entity.Container;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared helpers for driving the /api/v2/sync endpoints in tests, built around the fact that
 * the command chain is a single, global, append-only log (there is no per-test reset).
 * Every test must fetch the current head itself and use fresh random UUIDs for entities.
 */
public final class SyncTestSupport {

    public static final UUID ROOT_ID = Container.ROOT_ID;
    public static final String PAST = "2020-01-01T00:00:00";
    public static final String FUTURE = "3000-01-01T00:00:00";

    private SyncTestSupport() {
    }

    public static Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    /** Returns the id of the command currently at the tip of the (single, global) chain. */
    public static UUID currentHead() {
        Response response = RestAssured.given().get("/api/v2/sync/fetchCommands");
        response.then().statusCode(200);
        List<Map<String, Object>> entries = response.jsonPath().getList("$");
        Map<String, Object> last = entries.get(entries.size() - 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> command = (Map<String, Object>) last.get("command");
        return UUID.fromString((String) command.get("id"));
    }

    public static Response applyRaw(String head, List<CommandEntryDTO> entries) {
        var spec = RestAssured.given().contentType(ContentType.JSON).body(entries);
        if (head != null) {
            spec = spec.queryParam("head", head);
        }
        return spec.post("/api/v2/sync/applyCommands");
    }

    public static Response apply(UUID head, String commandType, Map<String, Object> payload) {
        return apply(head, commandType, 1, payload);
    }

    public static Response apply(UUID head, String commandType, Integer commandVersion, Map<String, Object> payload) {
        CommandDTO command = new CommandDTO();
        command.id = UUID.randomUUID();
        command.commandType = commandType;
        command.commandVersion = commandVersion;
        command.payload = payload;
        CommandEntryDTO entry = new CommandEntryDTO(head, null, command);
        return applyRaw(head == null ? null : head.toString(), List.of(entry));
    }

    /** Applies a single command, asserts it succeeds (200), and returns the new head. */
    public static UUID applyOk(UUID head, String commandType, Map<String, Object> payload) {
        Response response = apply(head, commandType, payload);
        response.then().statusCode(200);
        return UUID.fromString(response.getBody().asString());
    }

    /** Applies a single command and asserts it is rejected with the given status. */
    public static Response applyExpectStatus(UUID head, String commandType, Map<String, Object> payload, int status) {
        Response response = apply(head, commandType, payload);
        response.then().statusCode(status);
        return response;
    }

    public static Response applyExpectStatus(UUID head, String commandType, Integer commandVersion,
                                              Map<String, Object> payload, int status) {
        Response response = apply(head, commandType, commandVersion, payload);
        response.then().statusCode(status);
        return response;
    }

    public static Response fetchCommands(String since) {
        var spec = RestAssured.given();
        if (since != null) {
            spec = spec.queryParam("since", since);
        }
        return spec.get("/api/v2/sync/fetchCommands");
    }
}
