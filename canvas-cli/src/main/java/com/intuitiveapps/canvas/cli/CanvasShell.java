package com.intuitiveapps.canvas.cli;

import com.intuitiveapps.canvas.domain.exception.CanvasException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

/**
 * The read-evaluate-print loop.
 *
 * <p>Takes a {@link Reader} and a {@link Writer} rather than reaching for {@code System.in} and
 * {@code System.out}, so the whole application can be driven from a string in a test and its
 * output compared exactly. That is the difference between testing the product and testing a
 * fragment of it.
 *
 * <p>Note how little this class knows. It can create a canvas, apply a {@code Drawing}, and print.
 * It has no idea what a rectangle is - so a new shape never touches this file.
 */
public final class CanvasShell {

    private static final String PROMPT = "enter command: ";

    private final BufferedReader in;
    private final PrintWriter out;
    private final boolean echoInput;
    private final CanvasSession session = new CanvasSession();

    /**
     * @param echoInput when true, each line read is echoed after the prompt. Set when input is
     *                  piped rather than typed, so a redirected session still reads as a
     *                  conversation instead of a wall of unattributed output.
     */
    public CanvasShell(Reader in, Writer out, boolean echoInput) {
        this.in = new BufferedReader(in);
        this.out = new PrintWriter(out, true);
        this.echoInput = echoInput;
    }

    public void run() {
        printBanner();
        try {
            loop();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read from the terminal", e);
        } finally {
            out.flush();
        }
    }

    private void loop() throws IOException {
        while (true) {
            out.print(PROMPT);
            out.flush();

            String line = in.readLine();
            if (line == null) {
                out.println();      // end of input: Ctrl-D, or the end of a piped script
                return;
            }
            if (echoInput) {
                out.println(line);
            }

            try {
                Optional<Command> command = CommandParser.parse(line);
                if (command.isEmpty()) {
                    continue;
                }
                if (!execute(command.get())) {
                    return;
                }
            } catch (CommandException | CanvasException e) {
                // Every expected failure lands here, and the loop continues - a mistyped command
                // should not throw away the drawing. Unexpected exceptions are deliberately not
                // caught: a defect should surface rather than be swallowed.
                out.println("Error: " + e.getMessage());
            }
            out.println();
        }
    }

    /**
     * @return false if the application should stop
     */
    private boolean execute(Command command) {
        // An if/else chain rather than a pattern switch because this targets Java 17, where
        // switch patterns are still a preview feature.
        if (command instanceof Command.CreateCanvas create) {
            session.create(create.width(), create.height());
            printCanvas();
        } else if (command instanceof Command.Draw draw) {
            draw.drawing().applyTo(session.current());
            printCanvas();
        } else if (command instanceof Command.Help) {
            print(help());
        } else if (command instanceof Command.Quit) {
            return false;
        }
        return true;
    }

    private void printCanvas() {
        print(session.current().render());
    }

    private void printBanner() {
        out.println("Canvas ready. Create one with 'C w h', for example 'C 20 4'.");
        out.println("Type 'help' for the full list of commands, 'Q' to quit.");
        out.println();
    }

    private static List<String> help() {
        return List.of(
                "Commands:",
                "  C w h            create a new canvas, w wide and h tall",
                "  L x1 y1 x2 y2    draw a horizontal or vertical line",
                "  R x1 y1 x2 y2    draw a rectangle from two opposite corners",
                "  B x y c          bucket fill the area connected to (x,y) with the character c",
                "  Q                quit",
                "",
                "Coordinates start at 1 in the top left corner.");
    }

    private void print(List<String> lines) {
        lines.forEach(out::println);
    }
}
