#!/usr/bin/env bash
killall firefox
killall Xvfb

DISPLAY=":${1}"

Xvfb "${DISPLAY}" -ac &
export DISPLAY="${DISPLAY}"