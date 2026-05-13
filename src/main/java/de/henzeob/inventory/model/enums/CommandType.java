package de.henzeob.inventory.model.enums;

public enum CommandType {
    ITEM_CREATE, ITEM_UPDATE, ITEM_DELETE, ITEM_MOVE,
    CONTAINER_CREATE, CONTAINER_UPDATE, CONTAINER_DELETE, CONTAINER_MOVE,
    CATEGORY_CREATE, CATEGORY_UPDATE, CATEGORY_DELETE,
    IMAGE_UPLOAD, IMAGE_DELETE, IMAGE_SET_PRIMARY,
    SYNONYM_CREATE, SYNONYM_DELETE;

    public String entityType() {
        String name = this.name();
        if (name.startsWith("ITEM_")) return "ITEM";
        if (name.startsWith("CONTAINER_")) return "CONTAINER";
        if (name.startsWith("CATEGORY_")) return "CATEGORY";
        if (name.startsWith("IMAGE_")) return "IMAGE";
        if (name.startsWith("SYNONYM_")) return "SYNONYM";
        throw new IllegalStateException("Unknown entity type for command: " + name);
    }
}
