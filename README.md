# Drawing program

A console drawing program — create a canvas, draw lines and rectangles, bucket fill — with a
command line interface and a browser interface over the same core.

```bash
./start.sh          # command line application
./start-web.sh      # browser version at http://localhost:8080
./run-tests.sh      # the full test suite
```

Nothing else is needed. The first run downloads Maven and the dependencies and takes a minute or
two; after that it starts immediately.

---

## Contents

1. [Running it](#1-running-it)
2. [Instruction manual](#2-instruction-manual)
3. [How it is put together](#3-how-it-is-put-together)
4. [The decision the brief does not make for you](#4-the-decision-the-brief-does-not-make-for-you)
5. [Design decisions](#5-design-decisions)
6. [Special cases and how they are handled](#6-special-cases-and-how-they-are-handled)
7. [Assumptions and deviations](#7-assumptions-and-deviations)
8. [Testing](#8-testing)
9. [What I would do next](#9-what-i-would-do-next)

---

## 1. Running it

**Requirements:** a JDK (17 or newer) on the `PATH`, and internet access on the first run.

| Command | What it does |
|---|---|
| `./start.sh` | Builds and launches the CLI. This is the main deliverable. |
| `./start-web.sh` | Builds and launches the Spring Boot server; open <http://localhost:8080>. `PORT=9090 ./start-web.sh` to use another port. |
| `./run-tests.sh` | Runs all 76 tests. |

There is no build tool to install. The repository carries a **script-only Maven wrapper**
(`mvnw`), which downloads Maven itself on first use — so there is no `maven-wrapper.jar` and, as
the brief requires, **no binary of any kind is committed**.

**Every invocation starts clean.** The canvas lives in memory and nothing is written to disk, so
stopping the process is the only reset the system has. Restart it and there is no canvas.

Piping input works, which is how the transcripts in [SAMPLE-SESSION.md](SAMPLE-SESSION.md) were
produced:

```bash
printf 'C 20 4\nL 1 2 6 2\nB 10 3 o\nQ\n' | ./start.sh
```

---

## 2. Instruction manual

### Commands

| Command | Effect |
|---|---|
| `C w h` | Create a new canvas `w` wide and `h` tall. Replaces any existing drawing. |
| `L x1 y1 x2 y2` | Draw a horizontal or vertical line of `x` between the two points, inclusive. |
| `R x1 y1 x2 y2` | Draw the outline of a rectangle from two opposite corners. |
| `B x y c` | Bucket fill: recolour the area connected to `(x,y)` with the character `c`. |
| `H` / `help` / `?` | List the commands. |
| `Q` | Quit. Ctrl-D does the same. |

**Coordinates start at 1** in the top left corner, matching the brief's examples — `L 1 2 6 2`
draws along the second row, not the third.

Command letters may be typed in any case (`C`, `c`), but **the fill colour is taken exactly as
typed** — it is data, and folding its case would change the picture. Blank lines and lines
beginning with `#` are ignored, which makes scripted sessions readable.

### What the output means

```
enter command: R 14 1 18 3
----------------------      <- the frame is drawn at render time; it is not part of the canvas
|             xxxxx  |
|xxxxxx       x   x  |      <- 'x' is what lines and rectangles are drawn with
|     x       xxxxx  |
|     x              |
----------------------
```

The canvas is reprinted in full after every successful command, so what you see is always the
current state. A refused command prints `Error: ...` and changes nothing.

### The browser version

`./start-web.sh`, then <http://localhost:8080>. The canvas is drawn as a grid of clickable cells,
so you can **click the spot you want to fill** — the one thing a browser can offer that a terminal
cannot. The page keeps a running log in the same wording as the CLI, and a **"Draw the example"**
button replays the brief's session end to end. Two browser windows share one canvas and see each
other's strokes.

The REST API underneath it:

| Method | Path | Body |
|---|---|---|
| `POST` | `/api/canvas` | `{"width":20,"height":4}` |
| `GET` | `/api/canvas` | — the current canvas, 404 if none |
| `POST` | `/api/canvas/lines` | `{"x1":1,"y1":2,"x2":6,"y2":2}` |
| `POST` | `/api/canvas/rectangles` | `{"x1":14,"y1":1,"x2":18,"y2":3}` |
| `POST` | `/api/canvas/fills` | `{"x":10,"y":3,"colour":"o"}` |
| `DELETE` | `/api/canvas` | — discard the canvas, 204 |

```bash
curl -X POST localhost:8080/api/canvas \
     -H 'Content-Type: application/json' -d '{"width":20,"height":4}'
```

Every endpoint answers with the **whole canvas**, in two forms: `rows` (the cells, no border) for
a client laying the picture out itself, and `render` (exactly what the terminal prints). Drawing
is not an operation whose outcome the client can predict — a fill spreads however far the existing
strokes let it — so returning the picture means the browser never guesses and never needs a second
call.

Failures are RFC 9457 problem details carrying the domain's own message. See
[SAMPLE-SESSION.md §4](SAMPLE-SESSION.md#4-the-same-core-over-http) for worked examples.

---

## 3. How it is put together

```
                 ┌──────────────┐            ┌──────────────┐
   terminal ───► │  canvas-cli  │            │  canvas-web  │ ◄─── browser / HTTP
                 │  REPL, parser│            │ Spring Boot  │
                 │  formatting  │            │ REST + page  │
                 └──────┬───────┘            └──────┬───────┘
                        │                           │
                        └──────────┬────────────────┘
                                   ▼
                        ┌─────────────────────┐
                        │    canvas-domain    │   no dependencies at all
                        │  Canvas · Drawing   │   no Spring, no JSON, no logging
                        │ Line·Rectangle·Fill │
                        └─────────────────────┘
```

Three Maven modules, and the dependency arrows only ever point inwards:

| Module | Contains | Depends on |
|---|---|---|
| `canvas-domain` | The raster and the shapes. `Canvas`, `Drawing`, `Line`, `Rectangle`, `BucketFill`, `Point`. | **nothing** |
| `canvas-cli` | Command parsing, the REPL, output wording. | `canvas-domain` |
| `canvas-web` | Spring Boot, REST controller, error mapping, the HTML page. | `canvas-domain` |

`canvas-domain/pom.xml` has **no `<dependencies>` section**. That is not a stylistic preference —
it is a constraint enforced by the build. The drawing rules cannot accidentally acquire a
dependency on a framework, because there is nowhere for one to come from. `canvas-cli` and
`canvas-web` cannot see each other, so either could be deleted without touching a line of the
core.

The payoff shows up in the tests: the domain's 31 tests run in well under a second with no
container, no HTTP and no mocking, because there is nothing to stand up.

### The brief says the functionality "might change in the future"

So the layout is chosen to make exactly that change cheap. Adding a shape — a circle, a diagonal
line, a filled rectangle, a triangle — is:

1. **one new class** in the domain implementing `Drawing`;
2. **one `case`** in `CommandParser` for its command letter;
3. **one method** in `CanvasController`, if it should be reachable over HTTP too.

`Canvas` does not change, because it knows how to hold and paint pixels and nothing about shapes.
`CanvasShell` does not change, because it only knows how to apply a `Drawing`. `CanvasService`
does not change, for the same reason. That is why `Command.Draw` carries a `Drawing` rather than
raw coordinates, and why `Drawing` is deliberately **not sealed**.

---

## 4. The decision the brief does not make for you

Most of this problem is straightforward, and the brief's sample session settles most of what is
left: that the border is rendered rather than stored, that a rectangle's corners are painted once
rather than twice, that a shape which does not fit is refused rather than clipped. Those were all
resolved by making `SpecificationExampleTest` pass without special cases.

One question it does **not** answer is what "connected" means for the bucket fill:

```
enter command: B 10 3 o
----------------------
|oooooooooooooxxxxxoo|
|xxxxxxooooooox   xoo|      <- the three cells inside the rectangle stay blank
|     xoooooooxxxxxoo|      <- and so does the pocket under the L, at (1,3)-(5,4)
|     xoooooooooooooo|
----------------------
```

It is tempting to read this picture as proof that the fill must be four-way connected — the flood
goes right round the rectangle without getting inside it. **It is not.** Both sealed regions here
are closed diagonally as well as orthogonally, so this example produces exactly the same picture
under eight-way connectivity. I checked, rather than assuming.

The two only differ when a wall touches only at a corner:

```
|*x |          four-way: the corner (1,1) is cut off, and only it is filled
|x  |          eight-way: the fill would slip diagonally between the two x's
|   |                     and take the whole canvas
```

The implementation is **four-way**, for two reasons. It is what paint programs do, so it is what a
user expects. And it is the conservative choice: a boundary the user drew stays a boundary, where
eight-way squeezes through a join they could reasonably have believed was closed. Being wrongly
contained is easy to see and easy to undo; wrongly flooding the whole canvas destroys work.

Since the specification's example cannot pin this down, the invented case above is what pins it —
`DrawingTest.doesNotLeakThroughDiagonals`. The reasoning lives in `BucketFill`'s javadoc, next to
the code it justifies.

`SpecificationExampleTest` does assert the related claim that *is* the brief's: that the rectangle's
interior remains an independent region, which can be filled separately afterwards.

---

## 5. Design decisions

### The canvas knows nothing about shapes

`Canvas` holds a `char[][]` and can `paint` one cell. It has no `drawLine` and no `drawRectangle`.
Shapes are `Drawing` implementations that call `paint`. That split is what makes a new shape a new
class rather than another method on a class that grows forever — and it is why neither user
interface has to learn about the shape.

### The border is rendered, not stored

The frame is added at render time and is not in the raster. Storing it would mean every shape had
to remember not to overwrite it, and the fill would have to treat it as a wall by convention.
Keeping it out means the canvas contains only what the user actually drew — which is also what
lets the web front end lay out cells directly without stripping a frame off first.

### Drawing is all or nothing

`R 3 3 20 4` on a 10×4 canvas draws **nothing**, rather than the parts that fit. Shapes validate
their entire extent before painting a single cell, so a rejected command leaves the canvas exactly
as it found it. A half-drawn rectangle is the kind of state a user cannot undo and cannot explain.
[SAMPLE-SESSION.md §2](SAMPLE-SESSION.md#2-error-handling-and-edge-cases) shows this happening to a
canvas that already has a drawing on it.

The check is on the shape's *defining points* rather than every cell it covers — legitimate here
because a line and a rectangle are both bounded by their endpoints, and the canvas is itself a
rectangle anchored at `(1,1)`. The advantage is the error message: `(20,4) is outside the 10x4
canvas` names the coordinate the user typed, not whichever cell along the top edge happened to run
off first. `Drawing`'s javadoc states this as a contract, including the case where it does not
apply.

### The fill is an explicit queue, not recursion

The textbook recursive flood fill overflows the stack: on a 1000×1000 canvas the recursion can be
a million frames deep and the JVM gives out around ten thousand. An `ArrayDeque` moves that depth
onto the heap, where a million entries is unremarkable. `DrawingTest.handlesALargeArea` fills a
canvas large enough that the recursive version would die.

No separate "visited" set is needed either: painting a cell changes its colour, so it can no longer
match the target and cannot be enqueued twice. The one case that would spin forever — filling with
the colour already there — is caught up front and treated as a no-op.

### Coordinates are one-based, converted in exactly one place

The brief's commands are one-based, so the domain's `Point` is too. The translation to zero-based
array indices happens inside `Canvas` and nowhere else, rather than being sprinkled as `-1`
through every shape — which is where off-by-one bugs come from.

### Unchecked exceptions with one common supertype

Every drawing rule violation extends `CanvasException` and carries a message safe to show a user.
That lets each adapter translate the whole family at a single point: one `catch` in the shell, one
`@RestControllerAdvice` in the web module. A shape added next year gets sensible error handling in
both interfaces for free rather than falling through as a 500.

Unexpected exceptions are deliberately **not** caught. A defect should surface, not be swallowed.

### Session state lives in the adapter, not the domain

A `Canvas` is a picture; it has no opinion about whether anybody has created one yet. "No canvas
yet" is a statement about the conversation, so the CLI owns `CanvasSession` and the web module owns
the field in `CanvasService`. Putting it in the domain would have forced the CLI's single-canvas
assumption onto the web adapter, where it is the wrong shape the moment you want more than one
drawing.

### The lock is in the web module, not in `Canvas`

`Canvas` is not thread safe. A Spring bean is a singleton shared by every request thread, so two
concurrent fills would interleave reads and writes of the same `char[][]` and produce a corrupt
picture. `CanvasService` serialises access with a single `ReentrantLock`.

It lives there rather than inside `Canvas` because the CLI is one person at one terminal with no
contention at all — making the domain synchronise would charge it for a problem it does not have.
**The adapter that introduces concurrency is the adapter that pays for it.**

### The web layer does not serialise domain types

Returning `Canvas` directly would let Jackson publish `width`, `height` and whatever else the class
happens to expose today as a public API contract, making every later refactor of the domain a
breaking change for clients. `ApiModels.CanvasView` states what the API promises, and nothing more.

### The canvas has a maximum size

The brief sets no limit, but `C 2000000000 2000000000` would overflow or exhaust the heap, and
"the program died" is a worse answer than "that is too big". The bound is 1000 per side and a
million cells — far beyond anything a terminal can usefully display, and about 2MB.

### No library solves the problem

Spring provides HTTP; JUnit and AssertJ provide testing. The raster, the shapes and the flood fill
are written here.

---

## 6. Special cases and how they are handled

| Situation | Behaviour | Reasoning |
|---|---|---|
| `L` / `R` / `B` before any `C` | Refused, session continues | Nothing to draw on |
| `C` on an existing drawing | Starts a fresh blank canvas | The brief has no separate clear command; recreating is the clear |
| Diagonal line | Refused, naming both points | The brief says only horizontal and vertical are supported. Silently drawing an approximation, or silently drawing nothing, would both mislead |
| Line endpoints given in reverse order | Drawn | A line has no direction once drawn |
| Rectangle corners in any of the four orders | Drawn | There is exactly one rectangle with those two points as opposite corners |
| Line with both endpoints equal | A single cell | Degenerate, not wrong — it is what was asked for |
| Rectangle with a zero-width or zero-height side | Degrades to a line, or a cell | Falls out of composing it from four `Line`s; no special case in the code |
| Shape partly off the canvas | Refused, canvas untouched | See "all or nothing" above |
| Coordinate below 1 | Refused at construction | `(0,1)` does not exist in the addressing scheme at all |
| Canvas of `0` or negative size | Refused | Not a canvas |
| Canvas over 1000 per side or 1,000,000 cells | Refused with the bound stated | Better than an `OutOfMemoryError` |
| **Fill starting on a drawn line** | **Recolours that connected run of `x`** | What a paint program does when you click a line. The region is defined by the colour at the start point, not by which shape drew it — refusing would invent a rule the brief does not have |
| Fill with the colour already there | No-op | The canvas is already in the requested state; also what keeps the flood loop from running forever |
| Fill colour of `-` or `\|` | Allowed | It looks like the border but is unambiguous inside the frame |
| Fill colour of a space or a control character | Refused | A space is indistinguishable from an untouched cell; a control character corrupts the output |
| Multi-character colour (`B 3 3 oo`) | Refused, not truncated | If two were typed, at least one is not what was meant |
| Non-numeric coordinate, wrong argument count, unknown letter | Specific message, session continues | A typo is part of using a REPL, not a reason to lose the drawing |
| Lowercase command letters | Accepted | The letter is interface; rejecting `q` would be a rule with nothing behind it |
| Blank line or `#` comment | Ignored | Makes scripted sessions readable |
| Ctrl-D / end of piped input | Stops cleanly, as `Q` does | Reaching the end of input is not an error |

**On filling from a line.** This is the one place I could argue myself either way. Refusing would
be defensible on the grounds that a line is "not a region" — but the specification defines the fill
by the start point and gives no notion of shape membership afterwards, and every paint program in
existence recolours the line you click on. Implementing the stricter rule would also mean the
canvas had to remember which shape drew each cell, which is a substantially larger model for no
stated requirement.

---

## 7. Assumptions and deviations

1. **The brief's `R` argument description is treated as a description, not a restriction.** It
   says "upper left corner" and "lower right corner"; the implementation accepts any two opposite
   corners. Nothing is lost — the brief's own example still works unchanged — and there is no
   sensible error to report for the other ordering.

2. **The fill is four-way connected.** The brief does not say, and — contrary to the obvious
   reading — its sample output does not settle it either; that example gives the same picture
   under both. Four-way is a deliberate choice, argued in
   [§4](#4-the-decision-the-brief-does-not-make-for-you).

3. **The canvas is bounded** at 1000 per side and 1,000,000 cells, as above. The brief states no
   limit; refusing loudly beats dying.

4. **The output adds a two-line banner** at startup, a `help` command, and accepts `#` comments.
   Nothing required was removed: the canvas renderings are asserted verbatim, character for
   character, against the brief in `SpecificationExampleTest` and `CanvasShellTest`.

5. **The prompt is `enter command: `,** exactly as the brief shows it. When input is piped rather
   than typed, the shell echoes each line after the prompt, because the terminal is not doing it —
   without that a redirected session is a wall of pictures with no indication of what produced
   them. Interactive use is unaffected.

6. **The two interfaces format their output independently.** `canvas-cli` and `canvas-web` cannot
   depend on each other. Sharing the wording would mean a fourth module for a handful of lines —
   the wrong trade today, and an easy change if it started to drift. The pictures themselves cannot
   drift, because both render through `Canvas.render()`.

7. **Java 17 rather than 21.** `CanvasShell.execute` would read better as a pattern-matching
   `switch`, but switch patterns are still a preview feature in 17, and 17 is the widest LTS
   target. The `if`/`instanceof` chain there is the one place this shows.

8. **A whitespace colour is unreachable from the CLI** — the parser tokenises on whitespace, so
   there is no way to type one. It is still validated in the domain, because the HTTP API can send
   `{"colour":" "}` and the rule belongs with the canvas rather than with either adapter.

---

## 8. Testing

**76 tests.** `./run-tests.sh`

| Where | Count | What it covers |
|---|---|---|
| `SpecificationExampleTest` | 2 | The brief's sample session, replayed step by step, comparing the rendered canvas after every command |
| `DrawingTest` | 29 | Canvas creation and bounds, lines, rectangles, fill connectivity, degenerate shapes, atomicity |
| `CommandParserTest` | 24 | Every command, casing, whitespace, arity, unknown letters, delegation of validation |
| `CanvasShellTest` | 10 | The complete transcript compared line by line; error recovery; clean start |
| `CanvasControllerTest` | 11 | Routing, JSON shape, and domain failures mapped to 400/404/409 |

Three of these are worth calling out.

**`SpecificationExampleTest`** is the acceptance test for the exercise. Every ambiguous reading of
the brief was settled by making it pass without special cases, so if a change breaks it, it has
broken the requirement rather than the test.

**`CanvasShellTest.reproducesTheSampleTranscript`** drives the real application through its real
entry point and compares the entire transcript, line for line, against the brief. The unit tests
prove the algorithm; this proves the product. It is also why `CanvasShell` takes a `Reader` and a
`Writer` instead of reaching for `System.in` — testability was a design input, not an afterthought.

**`CanvasControllerTest.reproducesTheSpecificationExample`** replays the same example over HTTP and
expects the same picture. That is the assertion that the two interfaces genuinely share a core,
rather than each carrying its own copy of the rules. The web tests deliberately do **not** re-test
drawing rules; that would be the same assertions written twice in a much slower harness. They
assert only what the HTTP layer can get wrong.

---

## 9. What I would do next

Being explicit about the edges, since this is an exercise and not a product:

- **More than one canvas.** The obvious next requirement, and it is a small change: a canvas id in
  the path (`/api/canvases/{id}/lines`) and a map in `CanvasService` instead of a field. Nothing in
  the domain moves, which is the point of keeping session concepts out of it.
- **Undo.** Every command is already a `Drawing` object, so the history is a list of them and undo
  is replaying all but the last onto a blank canvas. Cheap now precisely because commands are
  values rather than method calls.
- **More shapes** — circles, filled rectangles, diagonal lines — following the three-step recipe in
  [§3](#3-how-it-is-put-together). The diagonal line is the interesting one, since it is the case
  the brief explicitly defers.
- **Persistence and export.** Everything is in memory by requirement. Writing a canvas out as text
  or PNG is a new adapter, not a domain change.
- **Structured logging** in the web module. Left out to keep the console readable for a demo, but
  the first thing a real deployment would want.
