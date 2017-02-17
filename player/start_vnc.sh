#!/usr/bin/env bash

x11vnc -ncache -localhost -display ":${1}" &
sleep 2
vncviewer -viewonly -truecolour -quality 9 localhost