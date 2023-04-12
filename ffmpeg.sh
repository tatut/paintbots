#!/bin/sh

# Example of how to convert images to video

ffmpeg -framerate 30 -i art_%06d.png -c:v libx264 -pix_fmt yuv420p art.mp4
