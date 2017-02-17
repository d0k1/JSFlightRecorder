#!/bin/bash
killall Xvfb
for i in `seq ${1} ${2}`; do
	Xvfb :$i -ac -dpms -noreset +extension GLX +render -shmem &
done
