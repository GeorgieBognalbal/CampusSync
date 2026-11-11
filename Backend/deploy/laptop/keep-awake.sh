#!/usr/bin/env bash
# Keeps the laptop from sleeping (even with the lid closed) while this runs.
# Use it during demos. Press Ctrl+C to allow sleep again.
echo "Laptop will stay awake (lid closed too). Press Ctrl+C to stop."
exec systemd-inhibit --what=sleep:idle:handle-lid-switch \
     --who="CampusSync" --why="Serving the CampusSync backend" \
     sleep infinity
