package com.intuitiveapps.canvas.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The outline of a rectangle, drawn with {@link Canvas#STROKE}.
 *
 * <p>The specification describes the arguments as the upper left and lower right corners. This
 * implementation accepts <strong>any two opposite corners</strong> and normalises them, so
 * {@code R 18 3 14 1} draws the same rectangle as {@code R 14 1 18 3}. Rejecting the other order
 * would be a rule with no purpose behind it; there is exactly one rectangle that has those two
 * points as opposite corners either way.
 *
 * <p>Degenerate cases are drawn rather than refused: a rectangle with equal x coordinates is a
 * vertical line, and one with both pairs equal is a single cell. Composing it from four
 * {@link Line}s makes that fall out for free instead of needing special cases.
 *
 * @param corner1 one corner
 * @param corner2 the opposite corner
 */
public record Rectangle(Point corner1, Point corner2) implements Drawing {

    @Override
    public void applyTo(Canvas canvas) {
        // The two corners bound the whole outline, so checking them is checking all of it - and it
        // means "R 2 2 9 9" on a small canvas is refused with "(9,9) is outside" rather than with
        // whichever cell along the top edge happened to run off first.
        Drawing.requireAllInside(canvas, List.of(corner1, corner2));
        points().forEach(point -> canvas.paint(point, Canvas.STROKE));
    }

    /** Every cell on the outline. Corners appear once - the four edges are de-duplicated. */
    public List<Point> points() {
        int left = Math.min(corner1.x(), corner2.x());
        int right = Math.max(corner1.x(), corner2.x());
        int top = Math.min(corner1.y(), corner2.y());
        int bottom = Math.max(corner1.y(), corner2.y());

        Point topLeft = new Point(left, top);
        Point topRight = new Point(right, top);
        Point bottomLeft = new Point(left, bottom);
        Point bottomRight = new Point(right, bottom);

        // A rectangle is four lines. Reusing Line rather than reimplementing the traversal means
        // the two shapes cannot disagree about what "inclusive of both endpoints" means.
        List<Point> outline = new ArrayList<>();
        outline.addAll(new Line(topLeft, topRight).points());
        outline.addAll(new Line(bottomLeft, bottomRight).points());
        outline.addAll(new Line(topLeft, bottomLeft).points());
        outline.addAll(new Line(topRight, bottomRight).points());

        return outline.stream().distinct().toList();
    }
}
