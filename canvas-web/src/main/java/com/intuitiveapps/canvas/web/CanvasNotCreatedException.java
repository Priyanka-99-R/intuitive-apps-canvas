package com.intuitiveapps.canvas.web;

/**
 * Raised when a request tries to draw before a canvas exists.
 *
 * <p>Lives in the web module rather than the domain, alongside the field it guards. "Has anybody
 * created a canvas yet?" is a question about the server's session, not about drawing: a
 * {@code Canvas} is a picture and has no opinion on whether it exists. The CLI answers the same
 * question in its own adapter, in {@code CanvasSession}, for the same reason.
 */
class CanvasNotCreatedException extends RuntimeException {

    CanvasNotCreatedException() {
        super("No canvas yet. Create one first with POST /api/canvas {\"width\":20,\"height\":4}.");
    }
}
