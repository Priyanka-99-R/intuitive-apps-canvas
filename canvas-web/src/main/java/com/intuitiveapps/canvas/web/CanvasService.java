package com.intuitiveapps.canvas.web;

import com.intuitiveapps.canvas.domain.Canvas;
import com.intuitiveapps.canvas.domain.Drawing;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Holds the server's canvas and serialises access to it.
 *
 * <h2>One shared canvas, not one per browser</h2>
 * <p>Unlike the ATM - where "who is logged in" is inherently per person - a canvas is a
 * <em>document</em>, and a single shared one makes the demo collaborative: open two browser
 * windows and each sees the other's strokes appear. It is also the honest analogue of the CLI,
 * which has exactly one canvas.
 *
 * <p>The extension, if this were real, is a canvas id in the path
 * ({@code /api/canvases/{id}/lines}) and a map here instead of a field. Nothing in the domain
 * would change, which is rather the point of keeping the domain free of session concepts.
 *
 * <h2>Why the lock is here rather than in Canvas</h2>
 * <p>{@link Canvas} is not thread safe, and a Spring bean is a singleton shared by every request
 * thread - two concurrent fills would interleave reads and writes of the same {@code char[][]}
 * and produce a corrupt picture. The CLI is one person at one terminal and has no contention at
 * all, so making the domain synchronise would charge it for a problem it does not have.
 * <strong>The adapter that introduces concurrency is the adapter that pays for it.</strong>
 */
@Service
public class CanvasService {

    private final ReentrantLock lock = new ReentrantLock();
    private Canvas canvas;

    /** Replaces any existing canvas with a new blank one. */
    public Canvas create(int width, int height) {
        return withLock(() -> {
            canvas = Canvas.of(width, height);
            return canvas;
        });
    }

    public Canvas current() {
        return withLock(this::requireCanvas);
    }

    /** Applies any {@link Drawing} - this method never needs to change when a shape is added. */
    public Canvas apply(Drawing drawing) {
        return withLock(() -> {
            Canvas target = requireCanvas();
            drawing.applyTo(target);
            return target;
        });
    }

    public void clear() {
        withLock(() -> {
            canvas = null;
            return null;
        });
    }

    private Canvas requireCanvas() {
        if (canvas == null) {
            throw new CanvasNotCreatedException();
        }
        return canvas;
    }

    /** The lock is released in a {@code finally}, so a rejected drawing cannot wedge the server. */
    private <T> T withLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
