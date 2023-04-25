# Prolog bot

Implementation of drawing bot with Prolog (tested with SWI-Prolog).

Run with `swipl bot.pl` then evaluate query `circle_demo("Somename").`.


## Toy Logo implementation

The code also contains an implementation of a Logo-like programming language
that can be used to create programmatic graphics with simple commands.

Start the REPL by evaluating `logo('botname').`

The language implements the following commands:
* `fd <N>` draw line forwards of length `N`
* `rt <N>` rotate `N` degrees
* `bk <N>` draw line backwards of length `N`
* `repeat <N> [ ...code... ]` repeat the given code N times
* `for [<V> <From> <To> <Step>] [ ...code... ]` repeat code with `V` getting each value from `From` to `To` (incremented by `Step` each round).
* `pen <C>` set pen color (0 - f)
* `randpen` set a random pen color
* `setxy <X> <Y>` move to X,Y coordinates (without drawing)

Parameter values can be integer numbers (possibly negative) or variable references prefixed with colon
(eg `:i`). Variables are all single characters.

Example programs:

Draw a spiral of lines:
`setxy 80 50 for [i 2 30 3] [randpen fd :i rt 80 fd :i rt 80 fd :i rt 80]`

Draw a star:
`repeat 5 [ fd 25 rt 144 ]`

Draw a circle of stars, each with a random color:
`repeat 6 [ randpen repeat 5 [ fd 25 rt 144 ] fd 30 rt 60]`
