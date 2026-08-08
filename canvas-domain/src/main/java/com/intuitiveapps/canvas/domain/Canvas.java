package com.intuitiveapps.canvas.domain;

import com.intuitiveapps.canvas.domain.exception.InvalidCanvasSizeException;
import com.intuitiveapps.canvas.domain.exception.InvalidColourException;
import com.intuitiveapps.canvas.domain.exception.PointOutsideCanvasException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A rectangular grid of characters that can be drawn on.
 *
 * <p>The canvas knows how to hold and render pixels. It deliberately does <em>not</em> know what a
 * line or a rectangle is - those are {@link Drawing} implementations that call {@link #paint}.
 * That split is what makes a new shape a new class rather than another method here, which matters
 * because the brief says the functionality "might change in the future".
 *
 * <p>Coordinates on the public API are one based, matching the specification's commands. The
 * translation to zero based array indices happens here and nowhere else.
 *
 * <p>Not thread safe; see the README on concurrency.
 */
public final class Canvas {

    /** An untouched cell. */
    public static final char BLANK = ' ';

    /** The character lines and rectangles are drawn with, per the specification. */
    public static final char STROKE = 'x';

    private static final char BORDER_HORIZONTAL = '-';
    private static final char BORDER_VERTICAL = '|';

    /**
     * Upper bounds on size.
     *
     * <p>The specification sets no limit, but an unbounded {@code C 2000000000 2000000000} would
     * either overflow or exhaust the heap, and "the program died" is a worse answer than "that is
     * too big". One million cells is far beyond anything a terminal can usefully display and
     * costs about 2MB.
     */
    private static final int MAX_DIMENSION = 1_000;
    private static final int MAX_CELLS = 1_000_000;

    private final int width;
    private final int height;
    private final char[][] pixels;

    private Canvas(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new char[height][width];
        for (char[] row : pixels) {
            Arrays.fill(row, BLANK);
        }
    }

    public static Canvas of(int width, int height) {
        if (width < 1 || height < 1) {
            throw new InvalidCanvasSizeException(
                    "Canvas must be at least 1x1; got " + width + "x" + height);
        }
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new InvalidCanvasSizeException(
                    "Canvas may be at most " + MAX_DIMENSION + " in each direction; got "
                            + width + "x" + height);
        }
        if ((long) width * height > MAX_CELLS) {
            throw new InvalidCanvasSizeException(
                    "Canvas may be at most " + MAX_CELLS + " cells; " + width + "x" + height
                            + " is " + (long) width * height);
        }
        return new Canvas(width, height);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(Point point) {
        return point.x() <= width && point.y() <= height;
    }

    public char colourAt(Point point) {
        requireInside(point);
        return pixels[point.y() - 1][point.x() - 1];
    }

    /**
     * Sets a single cell.
     *
     * @throws PointOutsideCanvasException if the point is off the canvas. Shapes are expected to
     *         validate their whole extent first, so reaching this is a defect rather than a user
     *         error - but it is checked anyway, because a shape that silently painted outside its
     *         bounds would corrupt the raster.
     */
    public void paint(Point point, char colour) {
        requireInside(point);
        pixels[point.y() - 1][point.x() - 1] = requireValidColour(colour);
    }

    /** Every point on the canvas, left to right then top to bottom. */
    public List<Point> allPoints() {
        List<Point> points = new ArrayList<>(width * height);
        for (int y = 1; y <= height; y++) {
            for (int x = 1; x <= width; x++) {
                points.add(new Point(x, y));
            }
        }
        return points;
    }

    /**
     * The drawn cells, one string per row, top to bottom and without the border.
     *
     * <p>A copy: the caller gets the picture, not a handle on the raster. The terminal wants
     * {@link #render()}, but a caller that is going to lay the cells out itself - the browser
     * front end draws one clickable element per cell - wants them without a border stitched on
     * that it would only have to strip off again.
     */
    public List<String> rows() {
        List<String> rows = new ArrayList<>(height);
        for (char[] row : pixels) {
            rows.add(new String(row));
        }
        return rows;
    }

    /**
     * Renders the canvas as lines of text, including the border.
     *
     * <p>The border is drawn at render time and is not stored in the raster. Storing it would mean
     * every shape had to remember not to overwrite it, and the fill algorithm would have to treat
     * it as a wall by convention. Keeping it out of the model means the canvas contains only what
     * the user drew.
     */
    public List<String> render() {
        String horizontalBorder = String.valueOf(BORDER_HORIZONTAL).repeat(width + 2);

        List<String> lines = new ArrayList<>(height + 2);
        lines.add(horizontalBorder);
        for (String row : rows()) {
            lines.add(BORDER_VERTICAL + row + BORDER_VERTICAL);
        }
        lines.add(horizontalBorder);
        return lines;
    }

    @Override
    public String toString() {
        return String.join(System.lineSeparator(), render());
    }

    /**
     * Colours must be a single visible character.
     *
     * <p>A space would be indistinguishable from an untouched cell, and a control character would
     * corrupt the rendered output. {@code -} and {@code |} are permitted: they look like the
     * border but they are unambiguous inside it, and inventing extra restrictions the
     * specification does not ask for would be worse than the mild confusion.
     */
    public static char requireValidColour(char colour) {
        if (Character.isWhitespace(colour) || Character.isISOControl(colour)) {
            throw new InvalidColourException(
                    "Colour must be a single visible character, not whitespace");
        }
        return colour;
    }

    private void requireInside(Point point) {
        if (!contains(point)) {
            throw new PointOutsideCanvasException(
                    point + " is outside the " + width + "x" + height + " canvas");
        }
    }
}
