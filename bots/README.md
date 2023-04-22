# Simple example bots

This folder contains example bot implementations.

* `shell` contains bash scripts using curl
* `python` a python example
* `node` a simple Node JavaScript example
* `typescript` a more complex Node TypeScript example
* `clojure` a Clojure example
* `java` a simple Java skeleton
*  `prolog` a Prolog sample


Simple scripts that use the paintbots API to draw stuff.

Bots use the `PAINTBOTS_URL` environment variable to read the URL
of the server. The ID of a registered bot is set to `PAINTBOTS_ID`
environment variable.

Example:
```shell
export PAINTBOTS_URL="https://localhost:31173"
export PAINTBOTS_ID=`./register.sh MazeBot`
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
