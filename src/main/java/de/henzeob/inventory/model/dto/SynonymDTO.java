package de.henzeob.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class SynonymDTO {

    public UUID id;

    @NotBlank(message = "Canonical term darf nicht leer sein")
    public String canonicalTerm;

    @NotBlank(message = "Synonym darf nicht leer sein")
    public String synonym;
}
