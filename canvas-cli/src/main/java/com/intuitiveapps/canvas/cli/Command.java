package com.intuitiveapps.canvas.cli;

import com.intuitiveapps.canvas.domain.Drawing;

/**
 * A parsed, validated instruction.
 *
 * <p>Separating parsing from execution means "is this line well formed?" and "can this be drawn?"
 * are answered in different places and tested independently. By the time a {@code Command} exists,
 * the text is syntactically valid and the coordinates have already become domain types.
 *
 * <p>Note that {@link Draw} carries a {@link Drawing} rather than raw numbers: the parser builds
 * the shape, so adding a shape means teaching the parser one more letter and writing one more
 * domain class. The shell does not grow a case for it.
 */
public sealed interface Command {

    /** {@code C w h} - replace the canvas with a new blank one. */
    record CreateCanvas(int width, int height) implements Command {
    }

    /** {@code L}, {@code R} and {@code B} all reduce to this. */
    record Draw(Drawing drawing) implements Command {
    }

    /** {@code Q} */
    record Quit() implements Command {
    }

    record Help() implements Command {
    }
}
