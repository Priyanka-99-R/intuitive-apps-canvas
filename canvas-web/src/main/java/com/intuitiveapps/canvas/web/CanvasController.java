package com.intuitiveapps.canvas.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP surface for the drawing program.
 *
 * <p>Every endpoint answers with the whole canvas rather than with "ok". Drawing is not a
 * request/response transaction the client can predict the outcome of - a fill spreads however far
 * the existing strokes let it - so returning the picture means the browser never has to guess what
 * changed, and never has to make a second call to find out.
 *
 * <p>The resources are named after shapes ({@code /lines}, {@code /rectangles}, {@code /fills})
 * rather than after the command letters. {@code POST /api/canvas/lines} says what it does to
 * somebody who has never read the brief; {@code POST /api/canvas/L} says nothing.
 *
 * <p>Adding a shape costs one record in {@link ApiModels} and one method here. It costs nothing in
 * {@link CanvasService}, which only knows how to apply a {@code Drawing}, and nothing in the
 * domain beyond the shape itself.
 *
 * <p>Note that there are no rules in this class. It converts HTTP to a method call and a canvas to
 * JSON; everything that could be called a decision lives in the domain.
 */
@RestController
@RequestMapping("/api/canvas")
class CanvasController {

    private final CanvasService canvasService;

    CanvasController(CanvasService canvasService) {
        this.canvasService = canvasService;
    }

    /**
     * {@code C w h}. Replaces any existing canvas, as it does in the terminal - starting again is
     * the only way to clear a drawing, and the brief has no separate command for it.
     */
    @PostMapping
    ApiModels.CanvasView create(@Valid @RequestBody ApiModels.CreateCanvasRequest request) {
        return ApiModels.toView(canvasService.create(request.width(), request.height()));
    }

    /** The current canvas, or 404 if nobody has created one. */
    @GetMapping
    ApiModels.CanvasView current() {
        return ApiModels.toView(canvasService.current());
    }

    /** {@code L x1 y1 x2 y2} */
    @PostMapping("/lines")
    ApiModels.CanvasView drawLine(@Valid @RequestBody ApiModels.LineRequest request) {
        return ApiModels.toView(canvasService.apply(ApiModels.toDomain(request)));
    }

    /** {@code R x1 y1 x2 y2} */
    @PostMapping("/rectangles")
    ApiModels.CanvasView drawRectangle(@Valid @RequestBody ApiModels.RectangleRequest request) {
        return ApiModels.toView(canvasService.apply(ApiModels.toDomain(request)));
    }

    /** {@code B x y c} */
    @PostMapping("/fills")
    ApiModels.CanvasView fill(@Valid @RequestBody ApiModels.FillRequest request) {
        return ApiModels.toView(canvasService.apply(ApiModels.toDomain(request)));
    }

    /**
     * Discards the canvas entirely, returning the server to the state it starts in.
     *
     * <p>This has no equivalent in the CLI, where quitting the process is the reset. A server
     * cannot be restarted between demonstrations, so it needs a way back to empty; 204 rather than
     * a body because there is no longer a canvas to describe.
     */
    @DeleteMapping
    ResponseEntity<Void> clear() {
        canvasService.clear();
        return ResponseEntity.noContent().build();
    }
}
