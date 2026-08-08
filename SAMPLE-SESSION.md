# Sample sessions

These transcripts were **captured by piping commands into the built application**, not written by
hand. Regenerate any of them with the command shown above it.

---

## 1. The session from the problem statement

Reproduced exactly. Every picture matches the brief, character for character. The same sequence is
asserted step by step in `SpecificationExampleTest` and again, as a whole transcript, in
`CanvasShellTest.reproducesTheSampleTranscript` — so it cannot silently drift.

```bash
printf 'C 20 4
L 1 2 6 2
L 6 3 6 4
R 14 1 18 3
B 10 3 o
Q
' | ./start.sh
```

```
Canvas ready. Create one with 'C w h', for example 'C 20 4'.
Type 'help' for the full list of commands, 'Q' to quit.

enter command: C 20 4
----------------------
|                    |
|                    |
|                    |
|                    |
----------------------

enter command: L 1 2 6 2
----------------------
|                    |
|xxxxxx              |
|                    |
|                    |
----------------------

enter command: L 6 3 6 4
----------------------
|                    |
|xxxxxx              |
|     x              |
|     x              |
----------------------

enter command: R 14 1 18 3
----------------------
|             xxxxx  |
|xxxxxx       x   x  |
|     x       xxxxx  |
|     x              |
----------------------

enter command: B 10 3 o
----------------------
|oooooooooooooxxxxxoo|
|xxxxxxooooooox   xoo|
|     xoooooooxxxxxoo|
|     xoooooooooooooo|
----------------------

enter command: Q
```

> The last command is the interesting one. The fill starts at `(10,3)`, outside the rectangle, and
> flows all the way around it — over the top, down the right hand side and back underneath — but
> the three cells inside it at `(15,2)`, `(16,2)`, `(17,2)` stay blank, and so does the pocket under
> the two lines at `(1,3)`–`(5,4)`.
>
> It is tempting to read that as proof the fill must be **four-way connected**. It is not: both
> regions here are sealed diagonally as well as orthogonally, so this picture comes out identical
> under eight-way connectivity. The brief's example does not decide the question — see
> [README §4](README.md#4-the-decision-the-brief-does-not-make-for-you) for the case that does,
> and why four-way is the choice.

---

## 2. Error handling and edge cases

Every one of these is refused with a specific, actionable message, and the session carries on with
the drawing intact. Nothing is silently ignored and nothing crashes.

```bash
printf 'L 1 1 5 1
C 0 5
C 2000 2000
C 10 4
R 2 2 8 4
L 1 1 5 5
R 3 3 20 4
B 30 1 o
L 0 1 5 1
L 1 1 5
B 3 3 oo
frobnicate
L a 1 5 1
B 1 1 .
B 1 1 .
Q
' | ./start.sh
```

```
Canvas ready. Create one with 'C w h', for example 'C 20 4'.
Type 'help' for the full list of commands, 'Q' to quit.

enter command: L 1 1 5 1
Error: No canvas yet. Create one first with 'C w h', e.g. 'C 20 4'.

enter command: C 0 5
Error: Canvas must be at least 1x1; got 0x5

enter command: C 2000 2000
Error: Canvas may be at most 1000 in each direction; got 2000x2000

enter command: C 10 4
------------
|          |
|          |
|          |
|          |
------------

enter command: R 2 2 8 4
------------
|          |
| xxxxxxx  |
| x     x  |
| xxxxxxx  |
------------

enter command: L 1 1 5 5
Error: Only horizontal or vertical lines are supported; (1,1) to (5,5) is diagonal

enter command: R 3 3 20 4
Error: (20,4) is outside the 10x4 canvas

enter command: B 30 1 o
Error: (30,1) is outside the 10x4 canvas

enter command: L 0 1 5 1
Error: Coordinates start at 1; got (0,1)

enter command: L 1 1 5
Error: Usage: L x1 y1 x2 y2

enter command: B 3 3 oo
Error: Colour must be a single character; got 'oo'

enter command: frobnicate
Error: Unknown command 'frobnicate'. Type 'help' to see the available commands.

enter command: L a 1 5 1
Error: 'a' is not a whole number (expected x)

enter command: B 1 1 .
------------
|..........|
|.xxxxxxx..|
|.x     x..|
|.xxxxxxx..|
------------

enter command: B 1 1 .
------------
|..........|
|.xxxxxxx..|
|.x     x..|
|.xxxxxxx..|
------------

enter command: Q
```

Three things worth pointing at:

- **`R 3 3 20 4` leaves the canvas completely untouched.** It is not drawn up to the edge and then
  abandoned — the next successful command shows the earlier rectangle still exactly as it was, with
  no stray cells at `(3,3)` or `(4,3)`. Shapes validate their whole extent before painting a single
  cell, so a command either happens or it does not. `CanvasShellTest.failedDrawingIsAtomic` asserts
  this directly.
- **The error names the coordinate the user typed** — `(20,4)`, not whichever cell along the top
  edge happened to run off first. That is why `Line` and `Rectangle` check their defining points
  rather than every cell they cover.
- **The second `B 1 1 .` is a no-op, not a hang.** The cell is already `.`, so there is nothing to
  spread from. Without that guard the flood loop would never terminate.

---

## 3. Degenerate shapes, corner order, and lowercase commands

Nothing here is a special case in the code — they all fall out of the design.

```bash
printf '# lowercase letters and comments are fine
c 12 5
l 6 4 6 2
r 10 4 8 2
r 2 2 2 2
b 6 3 A
b 1 1 -
c 12 5
Q
' | ./start.sh
```

```
Canvas ready. Create one with 'C w h', for example 'C 20 4'.
Type 'help' for the full list of commands, 'Q' to quit.

enter command: # lowercase letters and comments are fine
enter command: c 12 5
--------------
|            |
|            |
|            |
|            |
|            |
--------------

enter command: l 6 4 6 2
--------------
|            |
|     x      |
|     x      |
|     x      |
|            |
--------------

enter command: r 10 4 8 2
--------------
|            |
|     x xxx  |
|     x x x  |
|     x xxx  |
|            |
--------------

enter command: r 2 2 2 2
--------------
|            |
| x   x xxx  |
|     x x x  |
|     x xxx  |
|            |
--------------

enter command: b 6 3 A
--------------
|            |
| x   A xxx  |
|     A x x  |
|     A xxx  |
|            |
--------------

enter command: b 1 1 -
--------------
|------------|
|-x---A-xxx--|
|-----A-x x--|
|-----A-xxx--|
|------------|
--------------

enter command: c 12 5
--------------
|            |
|            |
|            |
|            |
|            |
--------------

enter command: Q
```

| Line | What it shows |
|---|---|
| `l 6 4 6 2` | Endpoints in either order. A drawn line has no direction, so bottom-to-top draws the same line as top-to-bottom. |
| `r 10 4 8 2` | Corners in either order. The brief says "upper left and lower right"; any two opposite corners describe the same rectangle, so all four orderings are accepted. |
| `r 2 2 2 2` | A rectangle with both corners equal is a single cell. It composes from four `Line`s, so this degrades rather than needing a special case. |
| `b 6 3 A` | Filling **from a drawn line** recolours that connected run of `x`, exactly as clicking a line in a paint program does. The region is defined by the colour at the start point, not by the shapes on the canvas. |
| `b 1 1 -` | `-` is allowed as a colour even though it looks like the border, because inside the frame it is unambiguous. Note the interior of the small rectangle at `(9,3)` stays blank — the fill went round it, never into it. |
| `c 12 5` | Creating again starts a fresh drawing. That is the only clear there is; the brief defines no separate command for it. |

---

## 4. The same core over HTTP

The browser version drives the identical domain code. Start it with `./start-web.sh`, then:

```bash
curl -s -X POST localhost:8080/api/canvas \
     -H 'Content-Type: application/json' -d '{"width":20,"height":4}'
curl -s -X POST localhost:8080/api/canvas/lines \
     -H 'Content-Type: application/json' -d '{"x1":1,"y1":2,"x2":6,"y2":2}'
curl -s -X POST localhost:8080/api/canvas/lines \
     -H 'Content-Type: application/json' -d '{"x1":6,"y1":3,"x2":6,"y2":4}'
curl -s -X POST localhost:8080/api/canvas/rectangles \
     -H 'Content-Type: application/json' -d '{"x1":14,"y1":1,"x2":18,"y2":3}'
curl -s -X POST localhost:8080/api/canvas/fills \
     -H 'Content-Type: application/json' -d '{"x":10,"y":3,"colour":"o"}'
```

The last response carries the same picture the terminal printed:

```json
{
  "width": 20,
  "height": 4,
  "rows": [
    "oooooooooooooxxxxxoo",
    "xxxxxxooooooox   xoo",
    "     xoooooooxxxxxoo",
    "     xoooooooooooooo"
  ],
  "render": [
    "----------------------",
    "|oooooooooooooxxxxxoo|",
    "|xxxxxxooooooox   xoo|",
    "|     xoooooooxxxxxoo|",
    "|     xoooooooooooooo|",
    "----------------------"
  ]
}
```

`CanvasControllerTest.reproducesTheSpecificationExample` replays this over HTTP and compares the
result against the same expected picture the CLI test uses — which is the proof that the two
interfaces really are sharing a core rather than each having their own copy of the rules.

Failures come back as RFC 9457 problem details, with the domain's own wording:

```bash
curl -s -X POST localhost:8080/api/canvas/lines \
     -H 'Content-Type: application/json' -d '{"x1":1,"y1":1,"x2":5,"y2":5}'
```

```json
{
  "type": "about:blank",
  "title": "Cannot be drawn",
  "status": 400,
  "detail": "Only horizontal or vertical lines are supported; (1,1) to (5,5) is diagonal",
  "instance": "/api/canvas/lines"
}
```
