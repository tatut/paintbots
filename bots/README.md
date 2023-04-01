# Simple bot scripts

Simple scripts that use the paintbots API to draw stuff.

Bots use the `PAINTBOTS` environment variable to read the URL
of the server.

Example:
```shell
export PAINTBOTS_URL="https://localhost:31173"
export PAINTBOTS_NAME="MazeBot"
# Draw a simple maze
./line.sh RIGHT 2
./line.sh UP 2
./line.sh LEFT 4
./line.sh DOWN 5
./line.sh RIGHT 6
./line.sh UP 7
./line.sh LEFT 8
# ...and so on
```
