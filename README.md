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

| Command  | Parameters      | Description                                                                               |
|----------|-----------------|-------------------------------------------------------------------------------------------|
| register | register=<name> | register a bot with the given name (if not already registered), returns id                |
| move     | move=<dir>      | move from current to position to direction <dir>, which is one of LEFT, RIGHT, UP or DOWN |
| paint    | no              | paint the current position with the current color                                         |
| color    | color=<col>     | set the current color to <col>, which is one of R,G,B,P,Y,P,C                             |
| msg      | msg=<msg>       | say <msg>, displays the message along with your name in the UI                            |
| clear    | no              | clear the current position                                                                |
|          |                 |                                                                                           |
