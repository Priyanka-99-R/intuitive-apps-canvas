package com.intuitiveapps.canvas.cli;

import com.intuitiveapps.canvas.domain.Canvas;

/**
 * The canvas currently being drawn on, if there is one.
 *
 * <p>Session state lives in the adapter, not the domain. A {@code Canvas} is a picture; it has no
 * opinion about whether anybody has created one yet. Putting "is there a canvas?" into the domain
 * would have forced the same single-canvas assumption onto the web adapter, where it is wrong the
 * moment two browsers connect.
 */
final class CanvasSession {

    private Canvas canvas;

    /** Replaces any existing canvas. `C` on an existing drawing starts again, as the brief implies. */
    void create(int width, int height) {
        canvas = Canvas.of(width, height);
    }

    boolean hasCanvas() {
        return canvas != null;
    }

    Canvas current() {
        if (canvas == null) {
            throw new CommandException("No canvas yet. Create one first with 'C w h', e.g. 'C 20 4'.");
        }
        return canvas;
    }
}
