package com.intuitiveapps.canvas.cli;

import com.intuitiveapps.canvas.domain.BucketFill;
import com.intuitiveapps.canvas.domain.Line;
import com.intuitiveapps.canvas.domain.Point;
import com.intuitiveapps.canvas.domain.Rectangle;
import com.intuitiveapps.canvas.domain.exception.UnsupportedLineException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CommandParserTest {

    @Test
    void parsesEveryCommand() {
        assertThat(CommandParser.parse("C 20 4")).contains(new Command.CreateCanvas(20, 4));
        assertThat(CommandParser.parse("L 1 2 6 2"))
                .contains(new Command.Draw(new Line(Point.of(1, 2), Point.of(6, 2))));
        assertThat(CommandParser.parse("R 14 1 18 3"))
                .contains(new Command.Draw(new Rectangle(Point.of(14, 1), Point.of(18, 3))));
        assertThat(CommandParser.parse("B 10 3 o"))
                .contains(new Command.Draw(new BucketFill(Point.of(10, 3), 'o')));
        assertThat(CommandParser.parse("Q")).contains(new Command.Quit());
    }

    @ParameterizedTest
    @ValueSource(strings = {"c 20 4", "C 20 4"})
    @DisplayName("command letters work in either case")
    void acceptsEitherCase(String line) {
        assertThat(CommandParser.parse(line)).contains(new Command.CreateCanvas(20, 4));
    }

    @Test
    @DisplayName("the fill colour keeps its case - only the command letter is folded")
    void preservesColourCase() {
        assertThat(CommandParser.parse("B 1 1 O"))
                .contains(new Command.Draw(new BucketFill(Point.of(1, 1), 'O')));
        assertThat(CommandParser.parse("B 1 1 o"))
                .isNotEqualTo(CommandParser.parse("B 1 1 O"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "# a comment"})
    void ignoresBlankLinesAndComments(String line) {
        assertThat(CommandParser.parse(line)).isEmpty();
    }

    @Test
    void handlesUntidyWhitespace() {
        assertThat(CommandParser.parse("   L    1   2    6   2  "))
                .contains(new Command.Draw(new Line(Point.of(1, 2), Point.of(6, 2))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C", "C 20", "C 20 4 5", "L 1 2 3", "R 1 2 3", "B 1 2", "B 1 2 o x", "Q now"})
    @DisplayName("the wrong number of arguments produces a usage message")
    void rejectsWrongArity(String line) {
        assertThatExceptionOfType(CommandException.class)
                .isThrownBy(() -> CommandParser.parse(line))
                .withMessageStartingWith("Usage:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"C x 4", "L 1 2 3 y", "B a 1 o", "C 1.5 2"})
    @DisplayName("non-integer coordinates are reported in plain language")
    void rejectsNonIntegers(String line) {
        assertThatExceptionOfType(CommandException.class)
                .isThrownBy(() -> CommandParser.parse(line))
                .withMessageContaining("is not a whole number");
    }

    @Test
    @DisplayName("a multi-character colour is refused rather than silently truncated")
    void rejectsMultiCharacterColour() {
        assertThatExceptionOfType(CommandException.class)
                .isThrownBy(() -> CommandParser.parse("B 1 1 oo"))
                .withMessageContaining("single character");
    }

    @Test
    void rejectsUnknownCommands() {
        assertThatExceptionOfType(CommandException.class)
                .isThrownBy(() -> CommandParser.parse("X 1 2"))
                .withMessageContaining("Unknown command 'X'");
    }

    @Test
    @DisplayName("shape validation is delegated to the domain, not duplicated in the parser")
    void delegatesShapeValidation() {
        assertThatExceptionOfType(UnsupportedLineException.class)
                .isThrownBy(() -> CommandParser.parse("L 1 1 3 3"));
    }

    @Test
    void parsesNullAsNothing() {
        assertThat(CommandParser.parse(null)).isEmpty();
    }
}
