package com.intuitiveapps.canvas.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The bucket fill tool: recolours the area connected to a starting point.
 *
 * <h2>What "connected" means here</h2>
 * Four-way connectivity - up, down, left and right, not diagonals.
 *
 * <p>This is a choice, not something the specification forces: its sample session happens to
 * produce the same picture either way, because nothing in it has a one-cell diagonal gap. The
 * case that separates the two is a wall that touches only at a corner, as in
 * {@code DrawingTest.doesNotLeakThroughDiagonals}, and four-way is the better answer there for
 * two reasons. It is what paint programs do, so it is what a user expects. And it is the
 * conservative reading: a boundary the user drew stays a boundary, where eight-way would squeeze
 * through a join the user could reasonably have believed was closed. Being wrongly contained is
 * an easy mistake to see and undo; wrongly flooding the canvas destroys work.
 *
 * <p>The area to recolour is defined by the <em>colour</em> at the starting point, not by the
 * shapes that happen to be on the canvas. Filling from a cell that sits on a drawn line therefore
 * recolours that connected run of {@code x} characters, which is exactly what a paint program
 * does when you click on a line. Refusing would be inventing a rule the specification does not
 * have.
 *
 * <h2>Why this is a loop and not a recursion</h2>
 * The obvious recursive flood fill overflows the stack: on a 1000x1000 canvas the recursion can
 * be a million frames deep, and the JVM's default stack gives out around ten thousand. An
 * explicit {@link ArrayDeque} moves that depth onto the heap, where a million entries is
 * unremarkable.
 *
 * <p>No separate "visited" set is needed. Painting a cell changes its colour, so it no longer
 * matches the target and cannot be enqueued twice. The one case that would spin forever - filling
 * with the colour that is already there - is caught up front.
 *
 * @param origin where the user clicked
 * @param colour the new colour
 */
public record BucketFill(Point origin, char colour) implements Drawing {

    public BucketFill {
        Canvas.requireValidColour(colour);
    }

    @Override
    public void applyTo(Canvas canvas) {
        Drawing.requireAllInside(canvas, List.of(origin));

        char target = canvas.colourAt(origin);
        if (target == colour) {
            // Already that colour. A no-op, not an error: the user asked for a state the canvas
            // is already in. Returning early is also what stops the loop below from running
            // forever, since painting would never change anything to dequeue.
            return;
        }

        Deque<Point> pending = new ArrayDeque<>();
        pending.add(origin);
        canvas.paint(origin, colour);

        while (!pending.isEmpty()) {
            Point point = pending.removeFirst();
            for (Point neighbour : neighboursOf(point)) {
                if (canvas.contains(neighbour) && canvas.colourAt(neighbour) == target) {
                    canvas.paint(neighbour, colour);   // paint on enqueue, so nothing is queued twice
                    pending.addLast(neighbour);
                }
            }
        }
    }

    /**
     * The four orthogonal neighbours that exist.
     *
     * <p>{@link Point} refuses coordinates below 1, so the guards here keep us from constructing
     * an invalid point rather than from stepping off the canvas - the caller checks the upper
     * bounds separately.
     */
    private static List<Point> neighboursOf(Point point) {
        int x = point.x();
        int y = point.y();

        List<Point> neighbours = new ArrayList<>(4);
        if (x > 1) {
            neighbours.add(new Point(x - 1, y));
        }
        if (y > 1) {
            neighbours.add(new Point(x, y - 1));
        }
        neighbours.add(new Point(x + 1, y));
        neighbours.add(new Point(x, y + 1));
        return neighbours;
    }
}
