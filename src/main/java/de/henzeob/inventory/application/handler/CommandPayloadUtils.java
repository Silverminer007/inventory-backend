package de.henzeob.inventory.application.handler;

import java.util.Map;
import java.util.UUID;

public final class CommandPayloadUtils {

    private CommandPayloadUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> T required(Map<String, Object> p, String key) {
        Object val = p.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required payload field: " + key);
        return (T) val;
    }

    public static UUID toUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    public static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    public static Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }
}
