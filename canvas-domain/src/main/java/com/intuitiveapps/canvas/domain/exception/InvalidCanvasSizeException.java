package com.intuitiveapps.canvas.domain.exception;

/** Raised when the requested canvas dimensions are not usable. */
public class InvalidCanvasSizeException extends CanvasException {

    public InvalidCanvasSizeException(String message) {
        super(message);
    }
}
