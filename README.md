# Paintbots!

A collaborative canvas where participants program "bots" that move on the canvas and color in cells.
The server contains a web UI to watch the canvas in (near) realtime to see how the art is being generated.

The server will also periodically save a PNG of the canvas that can be turned into a video at the end of
a session.

## Running

Start by running `clojure -m paintbots.main`. Then open browser at http://localhost:31173

See bots folder for sample bots using simple bash scripts.

## Bot commands

Each bot must be registered to use. All bot commands are POSTed using simple form encoding to
the server. See bots folder `_utils.sh` on how it uses curl to post commands.

| Command  | Parameters      | Description                                                                             |
|----------|-----------------|-----------------------------------------------------------------------------------------|
| register | register=name   | register a bot with the given name (if not already registered), returns id              |
| move     | id=ID&move=DIR  | move from current to position to direction DIR, which is one of LEFT, RIGHT, UP or DOWN |
| paint    | id=ID           | paint the current position with the current color                                       |
| color    | id=ID&color=COL | set the current color to COL, which is one of 0-f (16 color palette)                    |
| msg      | id=ID&msg=MSG   | say MSG, displays the message along with your name in the UI                            |
| clear    | id=ID&clear     | clear the pixel at current position                                                     |
| look     | id=ID           | look around, returns ascii containing the current image (with colors as above)          |
| bye      | id=ID&bye       | deregister this bot (id no longer is usable and name can be reused)                     |

Register command returns just the ID (as plain text) for future use. All other commands return your
bot's current position and color.
