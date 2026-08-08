package com.intuitiveapps.canvas.cli;

import com.intuitiveapps.canvas.domain.BucketFill;
import com.intuitiveapps.canvas.domain.Line;
import com.intuitiveapps.canvas.domain.Point;
import com.intuitiveapps.canvas.domain.Rectangle;

import java.util.Locale;
import java.util.Optional;

/**
 * Turns a line of text into a {@link Command}.
 *
 * <p>Pure and stateless - same input, same output, nothing to mock - which is what makes input
 * handling exhaustively testable on its own. That matters here because this is where most of the
 * awkward cases live.
 *
 * <p><strong>This class is the whole extension point for new shapes.</strong> Adding a circle
 * means writing {@code Circle implements Drawing} in the domain and adding one {@code case} here.
 * {@link CanvasShell} never changes, because it only knows how to apply a {@code Drawing}.
 *
 * <p>Command letters are accepted in either case: {@code C} and {@code c} both create a canvas.
 * The specification uses capitals, but rejecting a lowercase {@code q} would be a rule with
 * nothing behind it. Fill colours are of course left exactly as typed.
 */
public final class CommandParser {

    private CommandParser() {
    }

    /**
     * @return the parsed command, or empty for a blank line or a {@code #} comment
     * @throws CommandException if the letter is unknown, the argument count is wrong, or an
     *         argument is not a number
     * @throws com.intuitiveapps.canvas.domain.exception.CanvasException if the arguments are
     *         numbers but do not describe something drawable - a diagonal line, say
     */
    public static Optional<Command> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return Optional.empty();
        }

        String[] tokens = trimmed.split("\\s+");
        String letter = tokens[0].toUpperCase(Locale.ROOT);
        int arguments = tokens.length - 1;

        return Optional.of(switch (letter) {
            case "C" -> {
                requireArgumentCount(arguments, 2, "C w h");
                yield new Command.CreateCanvas(number(tokens[1], "w"), number(tokens[2], "h"));
            }
            case "L" -> {
                requireArgumentCount(arguments, 4, "L x1 y1 x2 y2");
                yield new Command.Draw(new Line(point(tokens, 1), point(tokens, 3)));
            }
            case "R" -> {
                requireArgumentCount(arguments, 4, "R x1 y1 x2 y2");
                yield new Command.Draw(new Rectangle(point(tokens, 1), point(tokens, 3)));
            }
            case "B" -> {
                requireArgumentCount(arguments, 3, "B x y c");
                yield new Command.Draw(new BucketFill(point(tokens, 1), colour(tokens[3])));
            }
            case "Q" -> {
                requireArgumentCount(arguments, 0, "Q");
                yield new Command.Quit();
            }
            case "H", "HELP", "?" -> new Command.Help();
            default -> throw new CommandException(
                    "Unknown command '" + tokens[0] + "'. Type 'help' to see the available commands.");
        });
    }

    private static Point point(String[] tokens, int index) {
        return Point.of(number(tokens[index], "x"), number(tokens[index + 1], "y"));
    }

    private static int number(String token, String name) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            // Rethrown as a CommandException so the shell reports it in the same voice as every
            // other input problem, rather than leaking a JDK exception message at the user.
            throw new CommandException("'" + token + "' is not a whole number (expected " + name + ")");
        }
    }

    /**
     * A colour is a single character.
     *
     * <p>{@code B 10 3 oo} is refused rather than silently using the first character - if the user
     * typed two, at least one of them is not what they meant.
     */
    private static char colour(String token) {
        if (token.length() != 1) {
            throw new CommandException(
                    "Colour must be a single character; got '" + token + "'");
        }
        return token.charAt(0);
    }

    private static void requireArgumentCount(int actual, int expected, String usage) {
        if (actual != expected) {
            throw new CommandException("Usage: " + usage);
        }
    }
}
