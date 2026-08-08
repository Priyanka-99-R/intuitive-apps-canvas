package com.intuitiveapps.canvas.domain.exception;

/**
 * Raised when a coordinate falls outside the canvas, or below 1.
 *
 * <p>Shapes validate every point they intend to touch <em>before</em> painting any of them, so a
 * rectangle that is half off the edge is refused outright rather than leaving two of its four
 * sides drawn.
 */
public class PointOutsideCanvasException extends CanvasException {

    public PointOutsideCanvasException(String message) {
        super(message);
    }
}
