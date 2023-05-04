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

## Perspective road

```
def linexyz(sx sy sz ex ey ez) {
  setxy 160-((160-:sx)/(:sz*0.1)) 100-((100-:sy)/(:sz*0.1))
  line  160-((160-:ex)/(:ez*0.1)) 100-((100-:ey)/(:ez*0.1))
}

def linexyz(sx sy sz ex ey ez) {
  setxy 160-((160-:sx)/(:sz*0.1)) 100-((100-:sy)/(:sz*0.1))
  line  160-((160-:ex)/(:ez*0.1)) 100-((100-:ey)/(:ez*0.1))
}

def road() {
  linexyz(80 200 10 80 200 1000)
  linexyz(240 200 10 240 200 1000)
  for [z 10 100 8] [
    linexyz(156 200 :z 156 200 :z+4)
    linexyz(156 200 :z+4 164 200 :z+4)
    linexyz(164 200 :z+4 164 200 :z)
    linexyz(156 200 :z 164 200 :z)
  ]
}

pen 7
road()

def housel(x y z w h d) {
  linexyz(:x+:w :y    :z    :x+:w :y-:h :z)
  linexyz(:x+:w :y-:h :z    :x    :y-:h :z)
  linexyz(:x    :y-:h :z    :x    :y    :z)
  linexyz(:x    :y    :z    :x+:w :y    :z)
  linexyz(:x+:w :y    :z    :x+:w :y    :z+:d)
  linexyz(:x+:w :y    :z+:d :x+:w :y-:h :z+:d)
  linexyz(:x+:w :y-:h :z+:d :x+:w :y-:h :z)
  linexyz(:x    :y-:h :z    :x    :y-:h :z+:d)
  linexyz(:x    :y-:h :z+:d :x+:w :y-:h :z+:d)
}


housel(35 200 10 30 25 4)

housel(35 200 16 30 45 5)

def linexyz(sx sy sz ex ey ez) {
  setxy 160-((160-:sx)/(:sz*0.1)) 100-((100-:sy)/(:sz*0.1))
  line  160-((160-:ex)/(:ez*0.1)) 100-((100-:ey)/(:ez*0.1))
}

/* draw some tree */
def tree(x y z) {
  pen 4 // draw trunk with brown
  linexyz(:x :y :z :x :y-rnd 10 15 :z)
  pen b // draw leafy branches with green
  savexy ax ay
  repeat 5 [
    setang rnd 240 290
    fd (100-:z)*0.04
    setxy :ax :ay
  ]
}

/* draw a lovely random forest  */
for [z 10 80 3] [
  repeat rnd 1 4 [
    tree(245 + rnd 5 20
         180 + rnd 0 20
         :z)
  ]
]

```

## Draw letters

All letter drawing functions expect to start at bottom left corner with angle 0.
The turtle is positioned after drawing to the next character (half size forward).

The functions take a single parameter s (for size, height).

```

def a(s) {
  rt -90 fd :s
  rt 90 fd :s/2
  rt 90 fd :s/2
  rt 90 fd :s/2 bk :s/2 rt -90 fd :s/2
  rt -90  }

def h(s) {
  rt -90 fd :s bk :s/2 rt 90 fd :s/2 rt -90 fd :s/2 rt 180
  fd :s rt -90
}

def e(s) {
  rt -90 fd :s rt 90
  repeat 2 [ fd :s/2 bk :s/2 rt 90 fd :s/2 rt -90 ]
  fd :s/2
}

def l(s) { rt -90 fd :s bk :s rt 90 fd :s/2 }
def o(s) { pu fd :s/2 rt 180 pd fd :s/2 rt 90 fd :s rt 90 fd :s/2 rt 90 fd :s rt -90 }
def t(s) { pu fd :s/2 pd rt -90 fd :s rt -90 pu fd :s/2 rt 180 pd fd :s rt 90 pu fd :s rt -90 pd }

def r(s) {
  pu fd :s/2 savexy rx ry bk :s/2 pd
  saveang ra
  rt -90 fd :s
  repeat 3 [ rt 90 fd :s/2 ]
  line :rx :ry
  setang :ra
}


/* underscore for space and spacing */
def _(s) { pu fd :s/2 pd }

setxy 10 70
setang 330
for [ c "hello_there" ] [ &c(10) _(10) rt 15 ]


## Simple face

Let's draw a simple round face  with eyes, nose, mouth, and some hair.


```
setxy 100 20 setang 0
def face() {
  savexy tx ty // save top of head position
  repeat 36 [fd 10 rt 10]
  rt 90 pu fd 50 rt 90 fd 20 rt 180
  pd fd 2  // eye1
  pu fd 50
  pd fd 2 // eye2
  pu bk 25 rt 90 fd 10
  pd rt 15 fd 20 rt -105 fd 5 // nose
  pu fd 25 rt 90 fd 5
  pd
  rt 40 repeat 50 [ fd 1 rt 2] // mouth
  /* random hair */
  repeat 20 [
    setxy :tx :ty
    setang rnd 0 180
    repeat 10 [ fd 4 rt rnd -15 15 ]
  ]
}

face()



```
