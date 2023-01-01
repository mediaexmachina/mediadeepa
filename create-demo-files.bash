#!/bin/bash
# This file is part of mediadeepa as internal and test source.
# It will generate some boring but specific audios and videos files.
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

A="-c:a pcm_s16le -y -hide_banner";
V="-y -hide_banner";

# Audio

# 0 sec => 5 sec
# Left: 1k sine (-36 dB)
# Right: noise (-21 dB)
ffmpeg -f lavfi -i "sine=frequency=1000:sample_rate=48000:duration=5" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=5:color=pink" \
       -filter_complex "[0:a][1:a]join=inputs=2:channel_layout=stereo,volume=volume=-18dB[a]" -map "[a]" $A sine-and-pink-stereo.wav

# 5 sec => 15 sec
# Left/Right: same/mono noise (-21 dB)
ffmpeg -f lavfi -i "anoisesrc=sample_rate=48000:duration=10:color=pink" \
       -filter_complex "[0:a][0:a]join=inputs=2:channel_layout=stereo,volume=volume=-18dB[a]" -map "[a]" $A pink-mono.wav

# 15 sec => 25 sec
# Left/Right: same noise out of phase (-21 dB)
ffmpeg -f lavfi -i "anoisesrc=sample_rate=48000:duration=10:color=pink,volume=volume=-18dB" \
       -filter_complex "[0:a]aeval='-val(0)':c=same[phaseout];[0:a][phaseout]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A pink-mono-outphase.wav

# 25 sec => 36 sec
# Left/Right: different/stereo noise in phase (-21 dB)
ffmpeg -f lavfi -i "anoisesrc=sample_rate=48000:duration=11:color=pink" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=11:color=pink" \
       -filter_complex "[0:a][1:a]join=inputs=2:channel_layout=stereo,volume=volume=-18dB[a]" -map "[a]" $A pink-stereo.wav

# 36 sec => 46 sec
# Left: saturated 1k sine/boxed waves (-18 dB)
# Right: noise as silence (-61 dB)
ffmpeg -f lavfi -i "sine=frequency=1000:sample_rate=48000:duration=10,volume=volume=24dB,aformat=sample_fmts=s16,volume=volume=-18dB" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=10,volume=volume=-61dB" \
       -filter_complex "[0:a][1:a]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A over-and-silence-stereo.wav

# 46 sec => 56 sec
# Left/Right: 1k sine with 10% DC offset (-18 dB)
ffmpeg -f lavfi -i "sine=frequency=1000:sample_rate=48000:duration=10,volume=volume=-18dB,dcshift=shift=0.1" \
       -filter_complex "[0:a][0:a]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A pink-mono-dc.wav

ffmpeg -i sine-and-pink-stereo.wav \
       -i pink-mono.wav \
       -i pink-mono-outphase.wav \
       -i pink-stereo.wav \
       -i over-and-silence-stereo.wav \
       -i pink-mono-dc.wav \
       -filter_complex "[0:a][1:a][2:a][3:a][4:a][5:a]concat=n=6:v=0:a=1[a]" \
       -map "[a]" $A demo-render.wav

rm -f sine-and-pink-stereo.wav
rm -f pink-mono.wav
rm -f pink-mono-outphase.wav
rm -f pink-stereo.wav
rm -f over-and-silence-stereo.wav
rm -f pink-mono-dc.wav

# Video

# 0 sec => 5 sec: smptebars
ffmpeg -f lavfi -i "smptebars=duration=5:size=cif:rate=25" -c:v ffv1 $V bars.mkv

# 5 sec => 10 sec: black frame
ffmpeg -f lavfi -i "color=color=black:duration=5:size=cif:rate=25" -c:v ffv1 $V black.mkv

# 10 sec => 20 sec: blured animated mandelbrot
ffmpeg -f lavfi -i "mandelbrot=start_scale=1:bailout=2:end_scale=0.001:size=cif:rate=25,boxblur=6:6" -t 00:00:10 -c:v ffv1 $V blur.mkv

# 20 sec => 21 sec: black frame
ffmpeg -f lavfi -i "color=color=black:duration=1:size=cif:rate=25" -c:v ffv1 $V small-black.mkv

# 21 sec => 31 sec: animated mandelbrot
ffmpeg -f lavfi -i "mandelbrot=start_scale=1:bailout=2:end_scale=0.001:size=cif:rate=25" -t 00:00:10 -c:v ffv1 $V mandelbrot.mkv

# 31 sec => 41 sec: animated mandelbrot with JPEG blocks/compression artefacts
ffmpeg -i "mandelbrot.mkv" -c:v mpeg1video -g 10 -b:v 300k -maxrate:v 300k -minrate:v 100k -bufsize:v 100k $V block.mpg

# 41 sec => 51 sec: rotated colored gradiant in a back box (letterboxed)
ffmpeg -f lavfi -i "gradients=speed=0.1:seed=1:c0=red:c1=blue:duration=10:rate=25:size=cif,crop=in_w:in_h-60:0:30,pad=in_w:in_h+60:0:-30" -c:v ffv1 $V blackbox.mkv

# 51 sec => 56 sec: interlaced (top frame first) animated mandelbrot
ffmpeg -f lavfi -i "mandelbrot=start_scale=0.01:bailout=2:end_scale=0.001:size=cif:rate=50,interlace" -t 00:00:05 -c:v ffv1 $V interlace.mkv

ffmpeg -i bars.mkv \
       -i black.mkv \
       -i blur.mkv \
       -i small-black.mkv \
       -i mandelbrot.mkv \
       -i block.mpg \
       -i blackbox.mkv \
       -i interlace.mkv \
       -filter_complex "[0:v][1:v][2:v][3:v][4:v][5:v][6:v][7:v]concat=n=8:v=1:a=0[vo]" \
       -map "[vo]" -c:v ffv1 $V video-render.mkv

rm -f bars.mkv
rm -f black.mkv
rm -f blur.mkv
rm -f small-black.mkv
rm -f mandelbrot.mkv
rm -f block.mpg
rm -f blackbox.mkv
rm -f interlace.mkv

# Mux

ffmpeg -i video-render.mkv -i demo-render.wav -c:v copy -c:a copy $V demo-render.mkv
rm -f video-render.mkv

ffmpeg -i demo-render.mkv -c:v copy -c:a copy $V demo-render.mov
ffmpeg -i demo-render.mkv -c:v copy -c:a copy $V demo-render.avi
ffmpeg -i demo-render.mkv -c:v mpeg2video -b:v 15M -c:a mp2 -b:a 256k $V demo-render.mpg
ffmpeg -i demo-render.mpg -c:v copy -c:a copy $V \
       -f mpegts -metadata service_provider="Media ex Machina" -metadata service_name="Demo render" demo-render.ts
ffmpeg -i demo-render.mkv -i demo-render.wav \
       -filter_complex "[1:a]channelsplit=channel_layout=stereo[ch1][ch2]" \
       -c:v mpeg2video -b:v 15M \
       -map "0:0" -map "[ch1]" -map "[ch2]" $V demo-render.mxf
