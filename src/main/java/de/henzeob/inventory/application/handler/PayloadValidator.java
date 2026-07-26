package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.exceptions.InvalidCommandPayloadException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Strict payload validation shared by all command handlers. DOMAIN-RULES.md requires that a
 * command is rejected as a whole if it contains any field outside the allowed set for its
 * command_type - this is what lets e.g. a stray created_at on an *_UPDATE command fail.
 */
public final class PayloadValidator {

    private PayloadValidator() {
    }

    public static void requireOnlyKeys(Map<String, Object> payload, Set<String> required, Set<String> optional) {
        for (String key : payload.keySet()) {
            if (!required.contains(key) && !optional.contains(key)) {
                throw new InvalidCommandPayloadException();
            }
        }
        for (String key : required) {
            if (payload.get(key) == null) {
                throw new InvalidCommandPayloadException();
            }
        }
    }

    public static UUID requireUUID(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) {
            throw new InvalidCommandPayloadException();
        }
        return parseUUID(val);
    }

    public static UUID optionalUUID(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) {
            return null;
        }
        return parseUUID(val);
    }

    private static UUID parseUUID(Object val) {
        try {
            return UUID.fromString(val.toString());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandPayloadException();
        }
    }

    public static String requireName(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (!(val instanceof String s) || s.isBlank() || s.length() < 3) {
            throw new InvalidCommandPayloadException();
        }
        return s;
    }

    /**
     * Returns null if the key is absent, validates as a name if present (used by *_UPDATE, where
     * the field is optional but must still satisfy the name constraints when supplied).
     */
    public static String optionalName(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return null;
        }
        return requireName(payload, key);
    }

    public static String optionalString(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        return val != null ? val.toString() : null;
    }

    public static int requireQuantity(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) {
            throw new InvalidCommandPayloadException();
        }
        return parseQuantity(val);
    }

    public static Integer optionalQuantity(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key) || payload.get(key) == null) {
            return null;
        }
        return parseQuantity(payload.get(key));
    }

    private static int parseQuantity(Object val) {
        int quantity;
        if (val instanceof Number n) {
            quantity = n.intValue();
        } else {
            try {
                quantity = Integer.parseInt(val.toString());
            } catch (NumberFormatException e) {
                throw new InvalidCommandPayloadException();
            }
        }
        if (quantity < 1) {
            throw new InvalidCommandPayloadException();
        }
        return quantity;
    }

    public static String requireShortCode(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (!(val instanceof String s) || s.isBlank() || s.length() > 4) {
            throw new InvalidCommandPayloadException();
        }
        return s;
    }

    public static String optionalShortCode(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) {
            return null;
        }
        return requireShortCode(payload, key);
    }

    public static Integer optionalHue(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key) || payload.get(key) == null) {
            return null;
        }
        int hue;
        Object val = payload.get(key);
        if (val instanceof Number n) {
            hue = n.intValue();
        } else {
            try {
                hue = Integer.parseInt(val.toString());
            } catch (NumberFormatException e) {
                throw new InvalidCommandPayloadException();
            }
        }
        if (hue < 0 || hue > 360) {
            throw new InvalidCommandPayloadException();
        }
        return hue;
    }

    public static LocalDateTime requireCreatedAtBeforeNow(Map<String, Object> payload, String key, Clock clock) {
        Object val = payload.get(key);
        if (!(val instanceof String s)) {
            throw new InvalidCommandPayloadException();
        }
        LocalDateTime createdAt;
        try {
            createdAt = LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            throw new InvalidCommandPayloadException();
        }
        if (!createdAt.isBefore(LocalDateTime.now(clock))) {
            throw new InvalidCommandPayloadException();
        }
        return createdAt;
    }
}
