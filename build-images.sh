#!/usr/bin/env bash
script_path=$(dirname "$0")

sudo docker buildx build --platform linux/arm64 --load -t ten1010io/project-controller:0.1.0-SNAPSHOT ${script_path}

