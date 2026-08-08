package com.intuitiveapps.canvas.domain.exception;

/**
 * Base type for every drawing rule the domain enforces.
 *
 * <p>Unchecked, with a message that is safe and useful to show a user. Giving the whole family a
 * common supertype is what lets each adapter translate it in one place - a single {@code catch}
 * in the shell, a single {@code @RestControllerAdvice} in the web module - instead of every call
 * site handling its own failures.
 */
public abstract class CanvasException extends RuntimeException {

    protected CanvasException(String message) {
        super(message);
    }
}
