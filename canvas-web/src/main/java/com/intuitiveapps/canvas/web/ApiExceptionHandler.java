package com.intuitiveapps.canvas.web;

import com.intuitiveapps.canvas.domain.exception.CanvasException;
import com.intuitiveapps.canvas.domain.exception.PointOutsideCanvasException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates domain failures into HTTP.
 *
 * <p>One place, for the whole family. This is the payoff for giving every rule violation a common
 * {@link CanvasException} supertype: no controller needs a {@code try/catch}, and a shape added
 * later gets sensible behaviour for free rather than falling through as a 500.
 *
 * <p>Responses use {@link ProblemDetail} (RFC 9457) rather than a hand-rolled error shape, so
 * clients get a documented, predictable envelope. The {@code detail} is the domain's own message,
 * which is the same sentence the terminal prints - one wording, two interfaces.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {
    // The explicit ordering matters. With spring.mvc.problemdetails.enabled=true, Spring registers
    // its own advice for the exceptions it knows about - including MethodArgumentNotValidException
    // - and without a declared precedence it is unspecified which advice wins. Highest precedence
    // makes this one authoritative, so the wording of an error is decided here rather than by
    // whichever advice happened to be consulted first.

    /** Nobody has created a canvas, so there is nothing at this resource yet. */
    @ExceptionHandler(CanvasNotCreatedException.class)
    ProblemDetail handleNoCanvas(CanvasNotCreatedException e) {
        return problem(HttpStatus.NOT_FOUND, "No canvas", e.getMessage());
    }

    /**
     * The shape is perfectly well described; it just does not fit <em>this</em> canvas. That is
     * 409 and not 400 - the request is unchanged and would succeed against a larger canvas, which
     * is exactly what "conflict with the current state of the resource" means.
     *
     * <p>Coordinates below 1 never reach here: they are malformed in any state, so
     * {@code @Positive} on the request rejects them as a 400 first.
     */
    @ExceptionHandler(PointOutsideCanvasException.class)
    ProblemDetail handleOutsideCanvas(PointOutsideCanvasException e) {
        return problem(HttpStatus.CONFLICT, "Does not fit the canvas", e.getMessage());
    }

    /**
     * Everything else the domain refuses - a diagonal line, an impossible canvas size, whitespace
     * as a colour - describes something that could never be drawn on any canvas. Changing the
     * server's state would not help, so it is the request that is wrong.
     *
     * <p>Listed after the more specific handler above, which Spring prefers on exact type match.
     */
    @ExceptionHandler(CanvasException.class)
    ProblemDetail handleDomainRule(CanvasException e) {
        return problem(HttpStatus.BAD_REQUEST, "Cannot be drawn", e.getMessage());
    }

    /** Bean-validation failures on the request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
