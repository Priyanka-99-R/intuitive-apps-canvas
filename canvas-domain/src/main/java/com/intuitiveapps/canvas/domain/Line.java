package com.intuitiveapps.canvas.domain;

import com.intuitiveapps.canvas.domain.exception.UnsupportedLineException;

import java.util.ArrayList;
import java.util.List;

/**
 * A horizontal or vertical line, drawn with {@link Canvas#STROKE}.
 *
 * <p>The endpoints may be given in either order: {@code L 6 4 6 3} draws the same line as
 * {@code L 6 3 6 4}. The specification's examples happen to go left-to-right and top-to-bottom,
 * but a line has no direction once drawn, so refusing the other order would be an arbitrary rule
 * for the user to remember.
 *
 * <p>A line whose endpoints are equal is a single cell. That is a degenerate case rather than an
 * error - it is exactly what the user asked for.
 *
 * @param from one endpoint, inclusive
 * @param to   the other endpoint, inclusive
 */
public record Line(Point from, Point to) implements Drawing {

    public Line {
        if (from.x() != to.x() && from.y() != to.y()) {
            throw new UnsupportedLineException(from, to);
        }
    }

    @Override
    public void applyTo(Canvas canvas) {
        // The endpoints, not every cell: a line between two points that are both on the canvas
        // cannot leave it in between, and refusing "L 3 2 9 2" with "(9,2) is outside" tells the
        // user something about what they typed. Reporting the first cell that happened not to fit
        // would name a coordinate they never mentioned.
        Drawing.requireAllInside(canvas, List.of(from, to));
        points().forEach(point -> canvas.paint(point, Canvas.STROKE));
    }

    /** Every cell this line covers, endpoints included. */
    public List<Point> points() {
        List<Point> points = new ArrayList<>();

        int startX = Math.min(from.x(), to.x());
        int endX = Math.max(from.x(), to.x());
        int startY = Math.min(from.y(), to.y());
        int endY = Math.max(from.y(), to.y());

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                points.add(new Point(x, y));
            }
        }
        // One of the two ranges is always a single value, because the constructor rejects
        // diagonals - so this loop walks a line, never an area.
        return points;
    }
}
