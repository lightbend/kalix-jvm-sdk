#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

PID_FILE=$1/.akkasls-pid

if [ -f "$PID_FILE" ]; then
    kill $(cat "$PID_FILE")
    rm "$PID_FILE"
fi
