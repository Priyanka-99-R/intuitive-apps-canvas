package com.intuitiveapps.canvas.cli;

/**
 * A problem with the interaction rather than with the drawing: an unknown command letter, the
 * wrong number of arguments, a coordinate that is not a number, or a command issued before a
 * canvas exists.
 *
 * <p>Kept separate from {@link com.intuitiveapps.canvas.domain.exception.CanvasException} because
 * none of these conditions exist in the domain. "You have not created a canvas yet" is meaningless
 * to a {@code Canvas} - it is a statement about the session, which belongs to whatever is holding
 * the conversation.
 */
public class CommandException extends RuntimeException {

    public CommandException(String message) {
        super(message);
    }
}
