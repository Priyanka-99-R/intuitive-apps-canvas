package com.intuitiveapps.canvas.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for the command line application.
 *
 * <p>The canvas is held only in memory. That is what satisfies the "clean start on every
 * invocation" requirement: there is no file and no cache, so stopping the process is the only
 * reset the system has or needs.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        // System.console() is null when input is piped. The terminal is then not echoing what was
        // typed, so the shell does it instead and the transcript stays readable.
        boolean interactive = System.console() != null;

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            Writer out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
            new CanvasShell(in, out, !interactive).run();
        }
    }
}
