package com.intuitiveapps.canvas.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the HTTP adapter: routing, JSON shape and the mapping from domain failures to status
 * codes.
 *
 * <p>The drawing rules are already covered by the domain module's tests and are not re-tested
 * here - that would be the same assertions written twice, in a much slower harness. What this
 * class asserts is the part only the web layer can get wrong, plus one end-to-end pass over the
 * specification's example to prove the two interfaces really do share a core.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CanvasControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CanvasService canvasService;

    /**
     * The service is a singleton shared by every test in the class, so each one starts by
     * discarding whatever the last left behind. Without this the tests would pass or fail
     * depending on the order JUnit happened to run them in.
     */
    @BeforeEach
    void startWithNoCanvas() {
        canvasService.clear();
    }

    private ResultActions send(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void createCanvas(int width, int height) throws Exception {
        send("/api/canvas", "{\"width\":%d,\"height\":%d}".formatted(width, height))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a new canvas is blank, and the border is rendered rather than drawn")
    void createReturnsABlankCanvas() throws Exception {
        send("/api/canvas", "{\"width\":3,\"height\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value(3))
                .andExpect(jsonPath("$.height").value(2))
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0]").value("   "))
                .andExpect(jsonPath("$.render.length()").value(4))
                .andExpect(jsonPath("$.render[0]").value("-----"))
                .andExpect(jsonPath("$.render[1]").value("|   |"));
    }

    @Test
    @DisplayName("the specification's example over HTTP gives the same picture as the terminal")
    void reproducesTheSpecificationExample() throws Exception {
        createCanvas(20, 4);
        send("/api/canvas/lines", "{\"x1\":1,\"y1\":2,\"x2\":6,\"y2\":2}").andExpect(status().isOk());
        send("/api/canvas/lines", "{\"x1\":6,\"y1\":3,\"x2\":6,\"y2\":4}").andExpect(status().isOk());
        send("/api/canvas/rectangles", "{\"x1\":14,\"y1\":1,\"x2\":18,\"y2\":3}")
                .andExpect(status().isOk());

        send("/api/canvas/fills", "{\"x\":10,\"y\":3,\"colour\":\"o\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0]").value("oooooooooooooxxxxxoo"))
                .andExpect(jsonPath("$.rows[1]").value("xxxxxxooooooox   xoo"))
                .andExpect(jsonPath("$.rows[2]").value("     xoooooooxxxxxoo"))
                .andExpect(jsonPath("$.rows[3]").value("     xoooooooooooooo"));
    }

    @Test
    @DisplayName("drawing before a canvas exists is 404, not 500")
    void drawingWithoutACanvasIsNotFound() throws Exception {
        send("/api/canvas/lines", "{\"x1\":1,\"y1\":1,\"x2\":2,\"y2\":1}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("No canvas"));

        mvc.perform(get("/api/canvas")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a shape that does not fit is 409 - the request is fine, the canvas is too small")
    void shapeOffTheCanvasIsConflict() throws Exception {
        createCanvas(5, 3);

        send("/api/canvas/rectangles", "{\"x1\":2,\"y1\":2,\"x2\":9,\"y2\":9}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Does not fit the canvas"))
                .andExpect(jsonPath("$.detail").value("(9,9) is outside the 5x3 canvas"));

        // and nothing was drawn - a refused shape is all or nothing
        mvc.perform(get("/api/canvas")).andExpect(jsonPath("$.rows[1]").value("     "));
    }

    @Test
    @DisplayName("a diagonal line is 400 - no canvas could ever accept it")
    void diagonalLineIsBadRequest() throws Exception {
        createCanvas(5, 5);

        send("/api/canvas/lines", "{\"x1\":1,\"y1\":1,\"x2\":3,\"y2\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cannot be drawn"))
                .andExpect(jsonPath("$.detail").value(
                        "Only horizontal or vertical lines are supported; (1,1) to (3,3) is diagonal"));
    }

    @Test
    @DisplayName("a coordinate below 1 is caught by validation, before the domain sees it")
    void nonPositiveCoordinateIsBadRequest() throws Exception {
        createCanvas(5, 5);

        send("/api/canvas/fills", "{\"x\":0,\"y\":1,\"colour\":\"o\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("x must be greater than 0"));
    }

    @Test
    @DisplayName("a multi-character colour is refused rather than silently truncated")
    void multiCharacterColourIsBadRequest() throws Exception {
        createCanvas(5, 5);

        send("/api/canvas/fills", "{\"x\":1,\"y\":1,\"colour\":\"oo\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("colour must be a single character"));
    }

    @Test
    @DisplayName("whitespace as a colour is refused by the domain, in the domain's words")
    void whitespaceColourIsBadRequest() throws Exception {
        createCanvas(5, 5);

        send("/api/canvas/fills", "{\"x\":1,\"y\":1,\"colour\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Colour must be a single visible character, not whitespace"));
    }

    @Test
    @DisplayName("an impossible canvas size is 400, with the domain's bounds rather than a 500")
    void impossibleCanvasSizeIsBadRequest() throws Exception {
        send("/api/canvas", "{\"width\":0,\"height\":4}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Canvas must be at least 1x1; got 0x4"));

        send("/api/canvas", "{\"width\":100000,\"height\":100000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Canvas may be at most 1000 in each direction; got 100000x100000"));
    }

    @Test
    @DisplayName("creating again starts a fresh drawing, as 'C' does in the terminal")
    void creatingAgainClearsTheDrawing() throws Exception {
        createCanvas(3, 1);
        send("/api/canvas/lines", "{\"x1\":1,\"y1\":1,\"x2\":3,\"y2\":1}")
                .andExpect(jsonPath("$.rows[0]").value("xxx"));

        send("/api/canvas", "{\"width\":3,\"height\":1}")
                .andExpect(jsonPath("$.rows[0]").value("   "));
    }

    @Test
    @DisplayName("delete returns the server to having no canvas at all")
    void deleteDiscardsTheCanvas() throws Exception {
        createCanvas(3, 1);

        mvc.perform(delete("/api/canvas")).andExpect(status().isNoContent());

        mvc.perform(get("/api/canvas")).andExpect(status().isNotFound());
    }
}
