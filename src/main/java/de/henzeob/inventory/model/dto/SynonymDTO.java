package de.henzeob.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;

public class SynonymDTO {

    public Long id;

    @NotBlank(message = "Canonical term darf nicht leer sein")
    public String canonicalTerm;

    @NotBlank(message = "Synonym darf nicht leer sein")
    public String synonym;
}
