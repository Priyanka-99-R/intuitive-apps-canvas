package com.intuitiveapps.canvas.domain;

import com.intuitiveapps.canvas.domain.exception.InvalidCanvasSizeException;
import com.intuitiveapps.canvas.domain.exception.InvalidColourException;
import com.intuitiveapps.canvas.domain.exception.PointOutsideCanvasException;
import com.intuitiveapps.canvas.domain.exception.UnsupportedLineException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Shapes, boundaries and the awkward cases. */
class DrawingTest {

    private static String render(Canvas canvas) {
        return String.join("\n", canvas.render());
    }

    @Nested
    @DisplayName("canvas")
    class CanvasCreation {

        @Test
        void startsBlankWithABorder() {
            assertThat(render(Canvas.of(3, 2))).isEqualTo("""
                    -----
                    |   |
                    |   |
                    -----""");
        }

        @Test
        @DisplayName("a 1x1 canvas is legal")
        void allowsTheSmallestCanvas() {
            assertThat(render(Canvas.of(1, 1))).isEqualTo("""
                    ---
                    | |
                    ---""");
        }

        @ParameterizedTest
        @CsvSource({"0,5", "5,0", "-1,5", "5,-1", "0,0"})
        @DisplayName("zero or negative dimensions are refused")
        void refusesEmptyCanvases(int width, int height) {
            assertThatExceptionOfType(InvalidCanvasSizeException.class)
                    .isThrownBy(() -> Canvas.of(width, height));
        }

        @Test
        @DisplayName("an absurdly large canvas is refused rather than exhausting the heap")
        void refusesHugeCanvases() {
            assertThatExceptionOfType(InvalidCanvasSizeException.class)
                    .isThrownBy(() -> Canvas.of(100_000, 100_000));
            assertThatExceptionOfType(InvalidCanvasSizeException.class)
                    .isThrownBy(() -> Canvas.of(1_000, 1_001));
        }

        @Test
        @DisplayName("the border is rendered, not stored - the raster holds only what was drawn")
        void borderIsNotPartOfTheRaster() {
            Canvas canvas = Canvas.of(3, 2);

            assertThat(canvas.colourAt(Point.of(1, 1))).isEqualTo(Canvas.BLANK);
            assertThat(canvas.width()).isEqualTo(3);
            assertThat(render(canvas).lines().findFirst()).contains("-----");

            // rows() is the same picture without the border - the web adapter draws its own
            assertThat(canvas.rows()).containsExactly("   ", "   ");
        }

        @Test
        @DisplayName("rows() hands out a copy, not a window onto the raster")
        void rowsAreASnapshot() {
            Canvas canvas = Canvas.of(3, 1);
            List<String> before = canvas.rows();

            new Line(Point.of(1, 1), Point.of(3, 1)).applyTo(canvas);

            assertThat(before).containsExactly("   ");
            assertThat(canvas.rows()).containsExactly("xxx");
        }
    }

    @Nested
    @DisplayName("line")
    class Lines {

        @Test
        void drawsHorizontally() {
            Canvas canvas = Canvas.of(5, 3);
            new Line(Point.of(2, 2), Point.of(4, 2)).applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    -------
                    |     |
                    | xxx |
                    |     |
                    -------""");
        }

        @Test
        void drawsVertically() {
            Canvas canvas = Canvas.of(3, 3);
            new Line(Point.of(2, 1), Point.of(2, 3)).applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    -----
                    | x |
                    | x |
                    | x |
                    -----""");
        }

        @Test
        @DisplayName("endpoints may be given in either order")
        void isDirectionless() {
            Canvas forwards = Canvas.of(5, 1);
            Canvas backwards = Canvas.of(5, 1);

            new Line(Point.of(1, 1), Point.of(4, 1)).applyTo(forwards);
            new Line(Point.of(4, 1), Point.of(1, 1)).applyTo(backwards);

            assertThat(render(forwards)).isEqualTo(render(backwards));
        }

        @Test
        @DisplayName("a line of length one is a single cell, not an error")
        void allowsADegenerateLine() {
            Canvas canvas = Canvas.of(3, 1);
            new Line(Point.of(2, 1), Point.of(2, 1)).applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    -----
                    | x |
                    -----""");
        }

        @Test
        void refusesDiagonals() {
            assertThatExceptionOfType(UnsupportedLineException.class)
                    .isThrownBy(() -> new Line(Point.of(1, 1), Point.of(3, 3)))
                    .withMessageContaining("diagonal");
        }

        @Test
        @DisplayName("a line that runs off the canvas draws nothing at all")
        void isAllOrNothing() {
            Canvas canvas = Canvas.of(5, 3);

            assertThatExceptionOfType(PointOutsideCanvasException.class)
                    .isThrownBy(() -> new Line(Point.of(3, 2), Point.of(9, 2)).applyTo(canvas))
                    // the endpoint the user gave, not the first cell along the way that ran off
                    .withMessage("(9,2) is outside the 5x3 canvas");

            // (3,2) and (4,2) were inside the canvas and must NOT have been painted
            assertThat(render(canvas)).isEqualTo("""
                    -------
                    |     |
                    |     |
                    |     |
                    -------""");
        }
    }

    @Nested
    @DisplayName("rectangle")
    class Rectangles {

        @Test
        void drawsAnOutline() {
            Canvas canvas = Canvas.of(6, 4);
            new Rectangle(Point.of(2, 2), Point.of(5, 4)).applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    --------
                    |      |
                    | xxxx |
                    | x  x |
                    | xxxx |
                    --------""");
        }

        @Test
        @DisplayName("any two opposite corners describe the same rectangle")
        void normalisesCorners() {
            Canvas fromTopLeft = Canvas.of(6, 4);
            Canvas fromBottomRight = Canvas.of(6, 4);

            new Rectangle(Point.of(2, 2), Point.of(5, 4)).applyTo(fromTopLeft);
            new Rectangle(Point.of(5, 4), Point.of(2, 2)).applyTo(fromBottomRight);

            assertThat(render(fromTopLeft)).isEqualTo(render(fromBottomRight));
        }

        @Test
        @DisplayName("a flat rectangle degrades to a line rather than failing")
        void allowsDegenerateRectangles() {
            Canvas canvas = Canvas.of(5, 3);
            new Rectangle(Point.of(2, 2), Point.of(4, 2)).applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    -------
                    |     |
                    | xxx |
                    |     |
                    -------""");
        }

        @Test
        @DisplayName("corners are painted once, not twice")
        void doesNotDuplicateCorners() {
            assertThat(new Rectangle(Point.of(1, 1), Point.of(3, 3)).points())
                    .hasSize(8)          // 3x3 outline = 9 cells minus the hollow centre
                    .doesNotHaveDuplicates();
        }

        @Test
        void refusesToDrawPartlyOffCanvas() {
            Canvas canvas = Canvas.of(5, 5);

            assertThatExceptionOfType(PointOutsideCanvasException.class)
                    .isThrownBy(() -> new Rectangle(Point.of(3, 3), Point.of(8, 8)).applyTo(canvas))
                    .withMessage("(8,8) is outside the 5x5 canvas");

            assertThat(render(canvas)).doesNotContain("x");
        }
    }

    @Nested
    @DisplayName("bucket fill")
    class Fill {

        @Test
        void fillsAnEmptyCanvas() {
            Canvas canvas = Canvas.of(3, 2);
            new BucketFill(Point.of(1, 1), 'z').applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    -----
                    |zzz|
                    |zzz|
                    -----""");
        }

        @Test
        @DisplayName("four-way connectivity - a diagonal gap does not leak")
        void doesNotLeakThroughDiagonals() {
            Canvas canvas = Canvas.of(3, 3);
            new Line(Point.of(1, 2), Point.of(1, 2)).applyTo(canvas);
            new Line(Point.of(2, 1), Point.of(2, 1)).applyTo(canvas);
            // x is at (2,1) and (1,2); the corner (1,1) is cut off from the rest orthogonally
            new BucketFill(Point.of(1, 1), '*').applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    -----
                    |*x |
                    |x  |
                    |   |
                    -----""");
        }

        @Test
        @DisplayName("filling with the colour already there is a no-op, not a hang")
        void isANoOpWhenNothingChanges() {
            Canvas canvas = Canvas.of(3, 2);
            new BucketFill(Point.of(1, 1), 'a').applyTo(canvas);

            new BucketFill(Point.of(2, 1), 'a').applyTo(canvas);   // must terminate

            assertThat(render(canvas)).isEqualTo("""
                    -----
                    |aaa|
                    |aaa|
                    -----""");
        }

        @Test
        @DisplayName("filling from a drawn line recolours the line, as a paint program would")
        void canFillFromALine() {
            Canvas canvas = Canvas.of(4, 2);
            new Line(Point.of(1, 1), Point.of(4, 1)).applyTo(canvas);

            new BucketFill(Point.of(2, 1), '#').applyTo(canvas);

            assertThat(render(canvas)).isEqualTo("""
                    ------
                    |####|
                    |    |
                    ------""");
        }

        @Test
        @DisplayName("a large fill completes without a stack overflow")
        void handlesALargeArea() {
            Canvas canvas = Canvas.of(1_000, 1_000);

            new BucketFill(Point.of(1, 1), 'q').applyTo(canvas);

            assertThat(canvas.colourAt(Point.of(1_000, 1_000))).isEqualTo('q');
        }

        @Test
        void refusesToStartOutsideTheCanvas() {
            Canvas canvas = Canvas.of(3, 3);

            assertThatExceptionOfType(PointOutsideCanvasException.class)
                    .isThrownBy(() -> new BucketFill(Point.of(9, 9), 'o').applyTo(canvas));
        }

        @Test
        void refusesWhitespaceAsAColour() {
            assertThatExceptionOfType(InvalidColourException.class)
                    .isThrownBy(() -> new BucketFill(Point.of(1, 1), ' '));
            assertThatExceptionOfType(InvalidColourException.class)
                    .isThrownBy(() -> new BucketFill(Point.of(1, 1), '\t'));
        }
    }

    @Test
    @DisplayName("coordinates below 1 are rejected at construction")
    void refusesNonPositiveCoordinates() {
        assertThatExceptionOfType(PointOutsideCanvasException.class)
                .isThrownBy(() -> Point.of(0, 1));
        assertThatExceptionOfType(PointOutsideCanvasException.class)
                .isThrownBy(() -> Point.of(1, -3));
    }
}
