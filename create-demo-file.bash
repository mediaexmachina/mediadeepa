#!/bin/bash

set -eu

mkdir -p target
cd target

A="-c:a pcm_s16le -y -hide_banner";
V="-y -hide_banner";

# Audio

ffmpeg -f lavfi -i "sine=frequency=1000:sample_rate=48000:duration=5" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=5" \
       -filter_complex "[0:a][1:a]join=inputs=2:channel_layout=stereo,volume=volume=-18dB[a]" -map "[a]" $A sine-and-pink-stereo.wav

ffmpeg -f lavfi -i "anoisesrc=sample_rate=48000:duration=10" \
       -filter_complex "[0:a][0:a]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A pink-mono.wav

ffmpeg -f lavfi -i "anoisesrc=sample_rate=48000:duration=10" \
       -filter_complex "[0:a]aphaseshift=shift=1.0[phaseout];[0:a][phaseout]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A pink-mono-outphase.wav

ffmpeg -f lavfi -i "anoisesrc=sample_rate=48000:duration=11" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=11" \
       -filter_complex "[0:a][1:a]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A pink-stereo.wav

ffmpeg -f lavfi -i "sine=frequency=1000:sample_rate=48000:duration=10,volume=volume=24dB,aformat=sample_fmts=s16,volume=volume=-18dB" \
       -f lavfi -i "anoisesrc=sample_rate=48000:duration=10,volume=volume=-60dB" \
       -filter_complex "[0:a][1:a]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A over-and-silence-stereo.wav

ffmpeg -f lavfi -i "sine=frequency=1000:sample_rate=48000:duration=10,volume=volume=-18dB,dcshift=shift=0.01" \
       -filter_complex "[0:a][0:a]join=inputs=2:channel_layout=stereo[a]" -map "[a]" $A pink-mono-dc.wav

ffmpeg -i sine-and-pink-stereo.wav \
       -i pink-mono.wav \
       -i pink-mono-outphase.wav \
       -i pink-stereo.wav \
       -i over-and-silence-stereo.wav \
       -i pink-mono-dc.wav \
       -filter_complex "[0:a][1:a][2:a][3:a][4:a][5:a]concat=n=6:v=0:a=1[a]" \
       -map "[a]" $A audio-render.wav

rm -f sine-and-pink-stereo.wav
rm -f pink-mono.wav
rm -f pink-mono-outphase.wav
rm -f pink-stereo.wav
rm -f over-and-silence-stereo.wav
rm -f pink-mono-dc.wav

# Video

ffmpeg -f lavfi -i "smptebars=duration=5:size=cif:rate=25" -c:v ffv1 $V bars.mkv
ffmpeg -f lavfi -i "color=color=black:duration=5:size=cif:rate=25" -c:v ffv1 $V black.mkv
ffmpeg -f lavfi -i "mandelbrot=start_scale=1:bailout=2:end_scale=0.001:size=cif:rate=25,boxblur=6:6" -t 00:00:10 -c:v ffv1 $V blur.mkv
ffmpeg -f lavfi -i "color=color=black:duration=1:size=cif:rate=25" -c:v ffv1 $V small-black.mkv
ffmpeg -f lavfi -i "mandelbrot=start_scale=1:bailout=2:end_scale=0.001:size=cif:rate=25" -t 00:00:10 -c:v ffv1 $V mandelbrot.mkv
ffmpeg -i "mandelbrot.mkv" -c:v mpeg1video -g 10 -b:v 300k -maxrate:v 300k -minrate:v 100k -bufsize:v 100k $V block.mpg
ffmpeg -f lavfi -i "gradients=speed=0.1:seed=1:c0=red:c1=blue:duration=10:rate=25:size=cif,crop=in_w:in_h-60:0:30,pad=in_w:in_h+60:0:-30" -c:v ffv1 $V blackbox.mkv
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

ffmpeg -i video-render.mkv -i audio-render.wav -c:v copy -c:a copy $V demo-render.mkv

rm -f video-render.mkv
rm -f audio-render.wav
