package com.intuitiveapps.canvas.domain;

import java.util.List;

/**
 * Something that can be applied to a canvas.
 *
 * <p>The extension point. The brief says the program's functionality "might change in the
 * future", so adding a shape - a circle, a diagonal line, a triangle - means writing one class
 * that implements this interface and registering its command letter in the parser. Neither
 * {@link Canvas} nor either user interface needs to change.
 *
 * <p>Deliberately <strong>not</strong> sealed, for exactly that reason: sealing would close the
 * set of shapes, and nothing in the system needs to switch over them exhaustively.
 *
 * <h2>The contract implementations must honour</h2>
 * Validate first, paint second. Nothing is painted until the whole shape is known to fit. A
 * rectangle that is half off the edge must be refused outright, not left with two of its four
 * sides drawn - a failed command should leave the canvas exactly as it found it.
 *
 * <p>A shape whose cells all fall within the bounding box of its defining points - which a line
 * and a rectangle both are - may check those points instead of every cell it covers. The two are
 * equivalent, since the canvas is itself a rectangle anchored at {@code (1,1)}, and checking the
 * defining points has the advantage of reporting the failure in the coordinates the user actually
 * typed. A shape that is <em>not</em> bounded that way must check every cell.
 */
public interface Drawing {

    /**
     * Applies this drawing.
     *
     * @throws com.intuitiveapps.canvas.domain.exception.CanvasException if the drawing does not
     *         fit or is otherwise not permitted. The canvas is left unchanged.
     */
    void applyTo(Canvas canvas);

    /**
     * Checks that every point lies on the canvas before any of them is painted.
     *
     * <p>Shared here rather than duplicated in each implementation, since "all or nothing" is a
     * rule of the interface rather than of any one shape.
     */
    static void requireAllInside(Canvas canvas, List<Point> points) {
        for (Point point : points) {
            if (!canvas.contains(point)) {
                throw new com.intuitiveapps.canvas.domain.exception.PointOutsideCanvasException(
                        point + " is outside the " + canvas.width() + "x" + canvas.height()
                                + " canvas");
            }
        }
    }
}
