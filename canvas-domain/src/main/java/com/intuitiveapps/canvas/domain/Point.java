package com.intuitiveapps.canvas.domain;

import com.intuitiveapps.canvas.domain.exception.PointOutsideCanvasException;

/**
 * A position on the canvas, <strong>one based</strong>: the top left cell is {@code (1,1)}.
 *
 * <p>One based because the specification's commands are - {@code L 1 2 6 2} draws along the top
 * of a canvas, not one row down. The conversion to the zero based array indices used internally
 * happens in exactly one place, {@link Canvas}, rather than being sprinkled as {@code -1} through
 * every shape.
 *
 * @param x column, counting from 1 at the left
 * @param y row, counting from 1 at the top
 */
public record Point(int x, int y) {

    public Point {
        if (x < 1 || y < 1) {
            throw new PointOutsideCanvasException(
                    "Coordinates start at 1; got (" + x + "," + y + ")");
        }
    }

    public static Point of(int x, int y) {
        return new Point(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
