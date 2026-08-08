package com.intuitiveapps.canvas.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replays the sample session from the problem statement, comparing the rendered canvas after
 * every command against the output the brief shows.
 *
 * <p>This is the acceptance test for the exercise. Ambiguities - whether the rectangle includes
 * its corners once or twice, where the border lives, whether a shape that does not fit is drawn
 * as far as it can be - were all settled by making this pass without special cases.
 *
 * <p>One ambiguity it does <em>not</em> settle is the fill's connectivity: this example produces
 * the same picture whether the fill is four-way or eight-way, because nothing in it has a
 * one-cell diagonal gap. That decision is argued in {@link BucketFill} and pinned by
 * {@code DrawingTest.doesNotLeakThroughDiagonals} instead.
 */
class SpecificationExampleTest {

    private static String render(Canvas canvas) {
        return String.join("\n", canvas.render());
    }

    @Test
    @DisplayName("the sample session from the problem statement, step by step")
    void replaysTheSampleSession() {
        // enter command: C 20 4
        Canvas canvas = Canvas.of(20, 4);
        assertThat(render(canvas)).isEqualTo("""
                ----------------------
                |                    |
                |                    |
                |                    |
                |                    |
                ----------------------""");

        // enter command: L 1 2 6 2
        new Line(Point.of(1, 2), Point.of(6, 2)).applyTo(canvas);
        assertThat(render(canvas)).isEqualTo("""
                ----------------------
                |                    |
                |xxxxxx              |
                |                    |
                |                    |
                ----------------------""");

        // enter command: L 6 3 6 4
        new Line(Point.of(6, 3), Point.of(6, 4)).applyTo(canvas);
        assertThat(render(canvas)).isEqualTo("""
                ----------------------
                |                    |
                |xxxxxx              |
                |     x              |
                |     x              |
                ----------------------""");

        // enter command: R 14 1 18 3
        new Rectangle(Point.of(14, 1), Point.of(18, 3)).applyTo(canvas);
        assertThat(render(canvas)).isEqualTo("""
                ----------------------
                |             xxxxx  |
                |xxxxxx       x   x  |
                |     x       xxxxx  |
                |     x              |
                ----------------------""");

        // enter command: B 10 3 o
        //
        // The fill starts outside the rectangle and flows all the way round it, but does NOT get
        // inside - the three cells at (15,2), (16,2), (17,2) stay blank - and the pocket below
        // the L of lines, at (1,3) to (5,4), stays blank too. Both regions are sealed
        // orthogonally AND diagonally, which is why this picture does not depend on the
        // connectivity choice.
        new BucketFill(Point.of(10, 3), 'o').applyTo(canvas);
        assertThat(render(canvas)).isEqualTo("""
                ----------------------
                |oooooooooooooxxxxxoo|
                |xxxxxxooooooox   xoo|
                |     xoooooooxxxxxoo|
                |     xoooooooooooooo|
                ----------------------""");
    }

    @Test
    @DisplayName("the interior of the rectangle can be filled separately, proving it was sealed")
    void interiorIsAnIndependentRegion() {
        Canvas canvas = Canvas.of(20, 4);
        new Line(Point.of(1, 2), Point.of(6, 2)).applyTo(canvas);
        new Line(Point.of(6, 3), Point.of(6, 4)).applyTo(canvas);
        new Rectangle(Point.of(14, 1), Point.of(18, 3)).applyTo(canvas);
        new BucketFill(Point.of(10, 3), 'o').applyTo(canvas);

        new BucketFill(Point.of(16, 2), '.').applyTo(canvas);

        assertThat(render(canvas)).isEqualTo("""
                ----------------------
                |oooooooooooooxxxxxoo|
                |xxxxxxooooooox...xoo|
                |     xoooooooxxxxxoo|
                |     xoooooooooooooo|
                ----------------------""");
    }
}
