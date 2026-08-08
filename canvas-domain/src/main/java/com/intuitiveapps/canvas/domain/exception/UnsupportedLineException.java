package com.intuitiveapps.canvas.domain.exception;

import com.intuitiveapps.canvas.domain.Point;

/**
 * Raised for a diagonal line.
 *
 * <p>The specification says "currently only horizontal or vertical lines are supported". Refusing
 * loudly is the honest reading: silently drawing an approximation, or silently drawing nothing,
 * would both leave the user believing something happened that did not.
 */
public class UnsupportedLineException extends CanvasException {

    public UnsupportedLineException(Point from, Point to) {
        super("Only horizontal or vertical lines are supported; " + from + " to " + to
                + " is diagonal");
    }
}
