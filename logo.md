# Some interesting toy logo programs

Some are adaptions from the excellent gallery at http://www.mathcats.com/gallery/15wordcontest.html

## Tree branches

This returns to the same position and draws randomly sprawling branches upwards.
```
repeat 10 [
  setxy 50 150
  setang rnd 220 320
  repeat 10 [ fd 2 rt rnd -15 15]
]
```

## Circle of starts

Draw 6 stars in a circle.
```
repeat 6 [
  randpen
  repeat 5 [ fd 25 rt 144 ]
  fd 30 rt 60
]
```

## Hypercube

Adapted from (http://www.mathcats.com/gallery/15wordcontest.html#lissajous)
```
repeat 8 [repeat 4 [rt 90 fd 25] bk 25 rt -45]
```

## Penta spiral

```
setxy 160 100
for [l 0 95 3] [
  repeat 5 [randpen fd :l rt 144]
  fd :l
  rt 30
]
```

## Spiral curve

Define function to draw curve and use it.

```
setxy 160 100
def curve(n len rot) {
   repeat :n [ randpen fd :len rt :rot ]
}
curve(10 10 rnd 10 20)
curve(10 8 rnd 20 30)
curve(10 6 rnd 30 50)
```

Repeat from same starting point:
```repeat 10 [
 setxy 160 100
 setang rnd 0 360
def curve(n len rot) {
   repeat :n [ randpen fd :len rt :rot ]
}
curve(10 9 rnd 10 20)
curve(10 8 rnd 20 30)
curve(10 6 rnd 30 50)
]
```
