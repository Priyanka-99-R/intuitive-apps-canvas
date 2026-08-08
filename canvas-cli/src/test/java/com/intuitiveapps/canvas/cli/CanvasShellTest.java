package com.intuitiveapps.canvas.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the whole application through its real entry point and asserts on what a user would see.
 *
 * <p>The first test is the important one: it feeds in the exact command sequence from the problem
 * statement and compares the complete transcript, line for line. The domain tests prove the
 * pixels; this proves the product.
 */
class CanvasShellTest {

    private List<String> run(String input) {
        StringWriter output = new StringWriter();
        new CanvasShell(new StringReader(input), output, true).run();
        return output.toString().lines().toList();
    }

    @Test
    @DisplayName("the sample session produces exactly the transcript in the problem statement")
    void reproducesTheSampleTranscript() {
        List<String> transcript = run("""
                C 20 4
                L 1 2 6 2
                L 6 3 6 4
                R 14 1 18 3
                B 10 3 o
                Q
                """);

        assertThat(transcript).containsExactly(
                "Canvas ready. Create one with 'C w h', for example 'C 20 4'.",
                "Type 'help' for the full list of commands, 'Q' to quit.",
                "",
                "enter command: C 20 4",
                "----------------------",
                "|                    |",
                "|                    |",
                "|                    |",
                "|                    |",
                "----------------------",
                "",
                "enter command: L 1 2 6 2",
                "----------------------",
                "|                    |",
                "|xxxxxx              |",
                "|                    |",
                "|                    |",
                "----------------------",
                "",
                "enter command: L 6 3 6 4",
                "----------------------",
                "|                    |",
                "|xxxxxx              |",
                "|     x              |",
                "|     x              |",
                "----------------------",
                "",
                "enter command: R 14 1 18 3",
                "----------------------",
                "|             xxxxx  |",
                "|xxxxxx       x   x  |",
                "|     x       xxxxx  |",
                "|     x              |",
                "----------------------",
                "",
                "enter command: B 10 3 o",
                "----------------------",
                "|oooooooooooooxxxxxoo|",
                "|xxxxxxooooooox   xoo|",
                "|     xoooooooxxxxxoo|",
                "|     xoooooooooooooo|",
                "----------------------",
                "",
                "enter command: Q");
    }

    @Test
    @DisplayName("drawing before a canvas exists is refused with a useful message")
    void refusesToDrawWithoutACanvas() {
        assertThat(run("L 1 1 2 1\nR 1 1 2 2\nB 1 1 o\n"))
                .filteredOn(line -> line.startsWith("Error:"))
                .hasSize(3)
                .allMatch(line -> line.contains("Create one first with 'C w h'"));
    }

    @Test
    @DisplayName("bad input is reported and the drawing survives")
    void recoversFromBadInput() {
        List<String> transcript = run("""
                C 5 3
                L 1 1 3 3
                L 1 1
                L a b c d
                C 0 0
                B 1 1
                B 1 1 oo
                B 9 9 o
                R 1 1 9 9
                zzz
                L 1 1 3 1
                Q
                """);

        assertThat(transcript).contains(
                "Error: Only horizontal or vertical lines are supported; (1,1) to (3,3) is diagonal",
                "Error: Usage: L x1 y1 x2 y2",
                "Error: 'a' is not a whole number (expected x)",
                "Error: Canvas must be at least 1x1; got 0x0",
                "Error: Usage: B x y c",
                "Error: Colour must be a single character; got 'oo'",
                "Error: (9,9) is outside the 5x3 canvas",
                "Error: Unknown command 'zzz'. Type 'help' to see the available commands.");

        // The canvas created at the start survived every one of those, and the final valid
        // command still drew on it.
        assertThat(transcript).containsSubsequence(
                "enter command: L 1 1 3 1",
                "|xxx  |");
    }

    @Test
    @DisplayName("a failed rectangle leaves the canvas untouched, not half drawn")
    void failedDrawingIsAtomic() {
        List<String> transcript = run("C 5 3\nR 2 2 9 9\nQ\n");

        assertThat(transcript)
                .contains("Error: (9,9) is outside the 5x3 canvas")
                .doesNotContain("| x   |");
    }

    @Test
    @DisplayName("creating a canvas again starts a fresh drawing")
    void recreatingClearsTheCanvas() {
        assertThat(run("C 3 1\nL 1 1 3 1\nC 3 1\nQ\n"))
                .containsSubsequence("enter command: C 3 1", "|xxx|", "enter command: C 3 1", "|   |");
    }

    @Test
    @DisplayName("each run starts with no canvas, as the brief requires")
    void startsClean() {
        run("C 5 5\nL 1 1 5 1\n");

        assertThat(run("L 1 1 2 1\n"))
                .anyMatch(line -> line.contains("No canvas yet"));
    }

    @Test
    @DisplayName("lowercase command letters work too")
    void acceptsLowercase() {
        assertThat(run("c 3 1\nl 1 1 3 1\nq\n")).contains("|xxx|");
    }

    @Test
    @DisplayName("blank lines and comments are ignored")
    void ignoresBlankLinesAndComments() {
        assertThat(run("\n  \n# draw a canvas\nC 3 1\nQ\n"))
                .containsSequence("enter command: C 3 1", "-----", "|   |", "-----");
    }

    @Test
    void helpListsEveryCommand() {
        assertThat(run("help\nQ\n"))
                .anyMatch(line -> line.contains("B x y c"))
                .anyMatch(line -> line.contains("R x1 y1 x2 y2"));
    }

    @Test
    @DisplayName("reaching the end of input stops cleanly, with or without Q")
    void handlesEndOfInput() {
        assertThat(run("C 3 1\n")).isNotEmpty();
    }
}
