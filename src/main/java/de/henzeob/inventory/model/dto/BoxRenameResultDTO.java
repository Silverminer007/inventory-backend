package de.henzeob.inventory.model.dto;

import java.util.ArrayList;
import java.util.List;

public class BoxRenameResultDTO {
    public List<RenameEntry> renames = new ArrayList<>();
    public int boxesRenamed;
    public int commandsApplied;
    public int commandsFailed;

    public static class RenameEntry {
        public String oldName;
        public String newName;
    }
}
