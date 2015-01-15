#!/bin/sh

java -cp "lib/*" etdprogram.Main "$@"  2>&1 | tee log.txt