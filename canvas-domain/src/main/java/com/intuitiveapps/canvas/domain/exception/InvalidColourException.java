package com.intuitiveapps.canvas.domain.exception;

/** Raised when a fill colour is not a single usable character. */
public class InvalidColourException extends CanvasException {

    public InvalidColourException(String message) {
        super(message);
    }
}
