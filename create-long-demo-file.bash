#!/bin/bash
# This file is part of mediadeepa as internal and test source.
# It will generate a very long and very boring video file, used for check application behavior with long files.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either command 3 of the License, or
# any later command.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# Copyright (C) Media ex Machina 2023
#
# Usage : just $0
# You need a recent (>v5) ffmpeg setup

set -eu

mkdir -p .demo-media-files
cd .demo-media-files

ffmpeg -f lavfi -i "gradients=speed=0.1:seed=1:c0=red:c1=blue:duration=14400:rate=25:size=sqcif" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=14400:color=pink,volume=volume=-18dB" \
       -preset ultrafast -profile:v baseline -pix_fmt yuv420p -g 10 \
       -c:v h264 -b:v 50k -c:a mp3 -b:a 32k -y -hide_banner long.mkv
