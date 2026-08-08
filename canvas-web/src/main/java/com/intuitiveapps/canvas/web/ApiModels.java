package com.intuitiveapps.canvas.web;

import com.intuitiveapps.canvas.domain.BucketFill;
import com.intuitiveapps.canvas.domain.Canvas;
import com.intuitiveapps.canvas.domain.Line;
import com.intuitiveapps.canvas.domain.Point;
import com.intuitiveapps.canvas.domain.Rectangle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The wire format, and the mapping between it and the domain.
 *
 * <p>{@link Canvas} is deliberately not serialised directly. Jackson would reach for its getters
 * and publish {@code width}, {@code height} and whatever else the class happens to expose today as
 * a public API contract, so every later refactor of the domain would become a breaking change for
 * clients. The view below says what the API promises, and nothing more.
 *
 * <h2>Where each rule is enforced</h2>
 * <p>Coordinates carry {@link Positive}, because a coordinate below 1 does not exist in the
 * addressing scheme at all - it is malformed regardless of what canvas is on the server, so it is
 * a 400 and bean validation can say so before the request reaches the domain.
 *
 * <p>{@code width} and {@code height} carry <em>no</em> annotation, on purpose. The domain already
 * has richer rules for them than {@code @Positive} could express - a minimum, a maximum per side
 * and a maximum cell count - and duplicating the first of the three here would mean two places
 * disagree about the wording the day one of them changes.
 */
final class ApiModels {

    private ApiModels() {
    }

    record CreateCanvasRequest(int width, int height) {
    }

    record LineRequest(@Positive int x1, @Positive int y1, @Positive int x2, @Positive int y2) {
    }

    record RectangleRequest(@Positive int x1, @Positive int y1,
                            @Positive int x2, @Positive int y2) {
    }

    /**
     * JSON has no character type, so the colour arrives as a string and is checked to be exactly
     * one character. {@code "oo"} is refused rather than truncated, matching the CLI - if the
     * caller sent two, at least one of them is not what they meant.
     */
    record FillRequest(@Positive int x, @Positive int y,
                       @NotNull @Size(min = 1, max = 1, message = "must be a single character")
                       String colour) {
    }

    /**
     * @param rows   the drawn cells without the border, one string per row - what a client that
     *               lays the canvas out itself wants
     * @param render the same picture as the terminal prints it, border included, so the two
     *               interfaces cannot drift apart in what they show
     */
    record CanvasView(int width, int height, List<String> rows, List<String> render) {
    }

    static CanvasView toView(Canvas canvas) {
        return new CanvasView(canvas.width(), canvas.height(), canvas.rows(), canvas.render());
    }

    static Line toDomain(LineRequest request) {
        return new Line(Point.of(request.x1(), request.y1()), Point.of(request.x2(), request.y2()));
    }

    static Rectangle toDomain(RectangleRequest request) {
        return new Rectangle(Point.of(request.x1(), request.y1()),
                Point.of(request.x2(), request.y2()));
    }

    static BucketFill toDomain(FillRequest request) {
        return new BucketFill(Point.of(request.x(), request.y()), request.colour().charAt(0));
    }
}
