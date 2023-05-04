# Paintbots!

A collaborative canvas where participants program "bots" that move on the canvas and color in cells.
The server contains a web UI to watch the canvas in (near) realtime to see how the art is being generated.

The server will also periodically save a PNG of the canvas that can be turned into a video at the end of
a session.

## Running

Start by running `clojure -m paintbots.main`. Then open browser at http://localhost:31173

See bots folder for sample bots using simple bash scripts.

Alternatively, you can run local server using Docker without needing to install Clojure:
`docker run -p 31173:31173 antitadex/paintbots:latest`

## Bot commands

Each bot must be registered to use. All bot commands are POSTed using simple form encoding to
the server. See bots folder `_utils.sh` on how it uses curl to post commands.

| Command  | Parameters      | Description                                                                             |
|----------|-----------------|-----------------------------------------------------------------------------------------|
| register | register=name   | register a bot with the given name (if not already registered), returns id              |
| info     | id=ID&info      | no-op command that just returns bots current                                            |
| move     | id=ID&move=DIR  | move from current to position to direction DIR, which is one of LEFT, RIGHT, UP or DOWN |
| paint    | id=ID           | paint the current position with the current color                                       |
| color    | id=ID&color=COL | set the current color to COL, which is one of 0-f (16 color palette)                    |
| msg      | id=ID&msg=MSG   | say MSG, displays the message along with your name in the UI                            |
| clear    | id=ID&clear     | clear the pixel at current position                                                     |
| look     | id=ID           | look around, returns ascii containing the current image (with colors as above)          |
| bye      | id=ID&bye       | deregister this bot (id no longer is usable and name can be reused)                     |
| bots     | id=ID&bots      | return (JSON) information about all registered bots                                     |

Register command returns just the ID (as plain text) for future use. All other commands return your
bot's current position and color.

## Deployment

See azure/README.md for instructions on how to deploy to Azure cloud. You can also easily deploy to
any cloud provider that supports hosting Docker images.


## Configuration

The default parameters in `config.edn` file are enough for most cases, but it is **highly** recommended
to change at least the admin password.

Notable configuration options:

* `:width` and `:height` affect the canvas size (320x200 or 160x100 are good to have something visible, bigger canvas will use more memory also)
* `:command-duration-ms` affects how long the processing of a single command will take at minimum (to prevent flooding with commands)
* `:password` admin UI password

See `config.edn` for full configuration with suitable sample values.

## Endpoints

HTTP endpoints in the running software:

* `GET /admin` the admin UI that lets you create/clear canvases and kick players (see above for configuring password)
* `GET /<canvas>` view a canvas with given name
* `POST /<canvas>` post a bot command to the given canvas

The canvas name in URL can only contain letters and the root path is a canvas named "scratch".
A canvas cannot be named "admin".

## Logo-like language

The UI includes a Logo-like language interpreter that can be used through the browser (tested with Chrome).
The evaluation uses the same API as all other bots, via the browser's `fetch`.

To start the interpreted, view the canvas page and add URL parameter `?client=1`.
For examples on programs see `logo.md` file in this repository.
