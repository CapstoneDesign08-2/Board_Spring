package com.board.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FoundBlankException extends Exception{
    public FoundBlankException() {
        super("Threre was Blanks");
    }
}