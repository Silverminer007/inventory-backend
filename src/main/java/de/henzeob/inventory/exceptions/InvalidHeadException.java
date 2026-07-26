package de.henzeob.inventory.exceptions;

import lombok.Getter;

@Getter
public class InvalidHeadException extends RuntimeException {
    private final String actualHead;

    public InvalidHeadException(String actualHead) {
        this.actualHead = actualHead;
    }
}
