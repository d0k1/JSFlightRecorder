#!/bin/bash 

cat "${1}"/*.png | ffmpeg -framerate 2 -f image2pipe -i - "${2}"/output.mkv