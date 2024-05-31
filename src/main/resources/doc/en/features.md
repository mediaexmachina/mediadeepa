Mediadeepa can handle any audio/video files managed by FFmpeg, and produce reports with it.

Analysis scope is currently based on FFmpeg filters and tools:
   - Audio phase meter `aphasemeter`
   - Time domain statistics about audio frames `astats`
   - Audio loudness meter (EBU R128 scanner) `ebur128`
   - Audio silence detection `silencedetect`
   - Video black/block artifacts/blurred/duplicate/interlacing frames detection (`blackdetect` / `blockdetect` / `blurdetect` / `freezedetect` / `idet`)
   - Video border detection `cropdetect`
   - Video spatial information (SI) and temporal information (TI) `siti`
   - Structural media container information: audio/video stream frames size, timing, GOP type
   - Create a technical resumed of media file based on FFprobe media file header
   - Optional filter use, based on command line (user choice) and current FFmpeg filter availability on app environment.
   - User can optionally add timed constraints:
     - start position on media file
     - limit analysis duration on media file
     - limit time to do the analysis operation
   - During the analyzing operation, an ETA/progress bar is displayed, and based on current FFmpeg processing

And it can export to file the FFprobe XML with media headers (container and A/V streams).

This application can run on three different "modes":
 - **Process to export**: this is the classical mode. Mediadeepa will drive FFmpeg to produce analysis data from your source file, and export the result a the end.
 - **Process to extract**: sometimes, you don't need to process data during the analysis session. So, Mediadeepa can just extract to raw text/xml files (zipped in one archive file) all the gathered data from FFmpeg.
 - **Import to export**: to load in Mediadeepa all gathered raw data files. Mediadeepa is **very tolerant** with the zip content, notably if they were not created by Mediadeepa (originally). **No one is mandatory in zip.**

You can process multiple files in one run, as well as load a text file as file list to process.

## Known limitations for Mediadeepa

 - It only support the first video, and the first founded audio stream of a file.
 - Audio mono and stereo only.
 - Some process take (long) time to do, like SITI and container analyzing, caused by poor FFmpeg/FFprobe performances with **these** filters.
 - Loudness EBU R-128,and _audio stats_ measures works correctly with FFmpeg v7+, due to internal bugs/limitations with the previous versions.

An internal warning will by displayed if you try to works with a Zip archive created by a different Mediadeepa version.
