# Media Deep Analysis

Audio/video medias and streams deep analyzer in Java with FFmpeg as back-end: extract/process technical information from audio/videos files/streams.

**This application is currently in alpha version, and should not be ready for production**

> [🚩 About](#🚩-about)\
> [🏪 Features](#🏪-features)\
> [⚡ Getting started](#⚡-getting-started)\
> [🛫 Usage](#🛫-usage)\
> [📕 Documentation](#📕-documentation)\
> [🎈 Road-map](#🎈-road-map)\
> [❤️ Contributing and support](#❤️-contributing-and-support)\
> [🙇 Author and copyright](#🙇-author-and-copyright)\
> [📜 License](#📜-license)\
> [🌹 Acknowledgments](#🌹-acknowledgments)

[![CodeQL](https://github.com/mediaexmachina/mediadeepa/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/mediaexmachina/mediadeepa/actions/workflows/codeql-analysis.yml)
[![Java CI with Maven](https://github.com/mediaexmachina/mediadeepa/actions/workflows/maven-package.yml/badge.svg)](https://github.com/mediaexmachina/mediadeepa/actions/workflows/maven-package.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mediaexmachina_mediadeepa&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=mediaexmachina_mediadeepa)

## 🚩 About

This application will run FFmpeg on a source video/audio file to apply some filters, and generate analysis raw data (mostly high verbosely text/XML streams). They are parsed and reduced/converted/drawn/summarized them to some output formats by Mediadeepa.

> Mediadeepa is a command line standalone application (no GUI, no specific setup).

## 🏪 Features

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
   - Export a FFprobe XML file based on media file headers
   - Create a technical resumed of media file based on FFprobe media file header
   - Optional filter use, based on command line (user choice) and current FFmpeg filter availability on app environment.
   - User can optionally add timed constraints:
     - start position on media file
     - limit analysis duration on media file
     - limit time to do the analysis operation
   - During the analyzing operation, an ETA/progress bar is displayed, and based on current FFmpeg processing

Export analysis format produce:
   - Plain text files (txt files, tabular separated)
   - CSV files
     - Classical: comma separated, "`.`" decimal separator.
     - French flavor: semicolon separated, "`,`" decimal separator.
   - Excel / XLSX Spreadsheet (simple raw data export, one tabs by export result)
   - SQLite
   - XML
   - JSON
   - Values displayed in charts:
     - Audio Loudness (integrated, short-term, momentary, true-peak), by time
     - Audio phase correlation (+/- 180°), by time
     - Audio DC offset, signal entropy, flatness, noise floor, peak level, silence/mono events, by time
     - Video quality as spatial information (SI) and temporal information
     - Video block/blur/black/interlacing/crop/freeze detection, by time
     - Video and audio iterate frames, by frame
     - Video frame duration stability, by frame
     - Video GOP (group of picture) size (number of frames by GOP, and by frame type), by GOP index.
     - Video GOP frame size, by frame type, by GOP, by frame number.
   - Useful HTML report with file and signal stats, event detection, codecs, GOP stats...

And it can export to file the FFprobe XML with media headers (container and A/V streams).

## ⚡ Getting started

### 🔩 Dependencies needed for run Mediadeepa

  - Java/JRE/JDK 17+
  - FFmpeg/FFprobe v5+

Declared on OS (Windows/Linux/macOS) PATH.

Download the last application release, as an executable JAR (autonomous *fat* JAR file), downloaded directly from [GitHub releases page](https://github.com/mediaexmachina/mediadeepa/releases), and build at each releases.

<details>
<summary>Or, you can build yourself this JAR, with Git and Maven.</summary>

Run on Linux/WSL/macOS, after setup Git and Maven:

```bash
git clone https://github.com/mediaexmachina/mediadeepa.git
cd mediadeepa
mvn install -DskipTests
```

Build jar will be founded on `target` directory as `mediadeepa-<version>.jar`

</details>

And simply run the application with

```bash
java -jar mediadeepa-<version>.jar
```

Mediadeepa contain embedded help, displayed with the `-h` parameter.

> You can set the command line parameters with `java -jar mediadeepa-<version>.jar [parameters]`.
>
> Example: `java -jar mediadeepa-1.0.0.jar -h`

## 🛫 Usage

This application can run on three different modes:
 - [Process to export](#🌿-process-to-export)
 - [Process to extract](#🌿-process-to-extract)
 - [Import to export](#🌿-import-to-export)

### 🌿 Process to export

This is the classical mode.

Mediadeepa will drive FFmpeg to produce analysis data from your source file, and export the result a the end.

Use these options with this mode:

```
-i, --input FILE           Input (media) file to process
-c, --container            Do a container analysing (FFprobe streams)
-f, --format FORMAT_TYPE   Format to export datas
-e, --export DIRECTORY     Export datas to DIRECTORY
```

All available **Export formats type** are listed by `-o`, and can be accumulated, like:

```
-f txt -f xlsx -f graphic
```

### 🌿 Process to extract

Sometimes, you don't need to process data during the analysis session.

So, Mediadeepa can just extract to raw text/xml files (zipped in one archive file) all the gathered data from FFmpeg.

Use this option with this mode:

```
--extract MEDIADEEPA_FILE        Extract all raw ffmpeg datas to a Mediadeepa archive file
```

### 🌿 Import to export

And now, how do you get process analyst with the archive file ?

With the third mode: import, to load in Mediadeepa all gathered raw data files. Mediadeepa is **very tolerant** with the zip content, notably if they were not created by Mediadeepa (originally). **No one is mandatory in zip.**

Use these options with this mode:

```
--import MEDIADEEPA_FILE        Import raw ffmpeg datas from a Mediadeepa archive file
-f, --format FORMAT_TYPE        Format to export datas
-e, --export DIRECTORY          Export datas to this directory
```

### 🎨 Options

<details><summary>
You can set some filter, analysis and export options.
</summary>

#### 🛒 Filter parameters

You can set specific technical values/thresholds/duration for some internal filters.

With options like:

```
--filter-ebur128-target DBFS
--filter-freezedetect-noisetolerance DB
--filter-freezedetect-duration SECONDS
--filter-idet-intl THRESHOLD_FLOAT
--filter-idet-prog THRESHOLD_FLOAT
--filter-idet-rep THRESHOLD_FLOAT
--filter-idet-hl FRAMES
[...]
```

Refer to the integrated command line help to get the full list.

Refer to [FFmpeg documentation](https://www.ffmpeg.org/ffmpeg-filters.html) to have more details on the works of each filter, and on the expected values.

No option are mandatory ; all will be empty and let to the default values to FFmpeg.

#### 🗜 Limit the scope of analysis

By default, all analysis options and filters are activated in relation to your FFmpeg setup. Container analysis is still optional (via `-c`).

Not all options are necessarily useful for everyone and all the time (like [crop detect](https://ffmpeg.org/ffmpeg-filters.html#cropdetect)), the processing of certain filters can be very resource intensive (like [SITI](https://ffmpeg.org/ffmpeg-filters.html#siti-1)), and/or produce a large amount of data.

First, you can list all active filters with the `-o` option:

```
Detected (and usable) filters:
amerge         Merge two or more audio streams into a single multi-channel stream.
ametadata      Manipulate audio frame metadata.
aphasemeter    Convert input audio to phase meter video output.
astats         Show time domain statistics about audio frames.
channelmap     Remap audio channels.
channelsplit   Split audio into per-channel streams.
ebur128        EBU R128 scanner.
join           Join multiple audio streams into multi-channel output.
silencedetect  Detect silence.
volumedetect   Detect audio volume.
blackdetect    Detect video intervals that are (almost) black.
blockdetect    Blockdetect filter.
blurdetect     Blurdetect filter.
cropdetect     Auto-detect crop size.
freezedetect   Detects frozen video input.
idet           Interlace detect Filter.
mestimate      Generate motion vectors.
metadata       Manipulate video frame metadata.
siti           Calculate spatial information (SI) and temporal information (TI).
```

> The description of each filter comes from the return of FFmpeg command.

And you can choose the analysis processing with these options:

```bash
-c, --container            Do a container analysing (ffprobe streams)
[...]
-fo, --filter-only FILTER  Allow only this filter(s) to process (-o to get list)
-fn, --filter-no FILTER    Not use this filter(s) to process (-o to get list)
-mn, --media-no            Disable media analysing (ffmpeg)
-an, --audio-no            Ignore all video filters
-vn, --video-no            Ignore all audio filters
```

#### 📦 Export formats

With the integrated help, you can get the export features currently available, to use with `-f`.

```
[...]
Export formats available:
txt            Values separated by tabs in text files
csv            Classic CSV files
csvfr          French flavor CSV files
xlsx           XLSX Spreadsheet
sqlite         SQLite database
xml            XML Document
json           JSON Document
html           HTML report document
ffprobexml     Media file headers on FFprobe XML
[...]
```

You can use several exports formats, comma separated, like `-f txt,xml,json`.

_NB: exports formats like XML or JSON can produce very large files, which may take time to make, if want to use all filters/analysis scopes (up to 4 MB for a file less than 1 minute of media). It's not a problem for Mediadeepa, but it can be for you!_

The `-e` parameter set the target export directory to put the produced files.

The `--export-base-filename` set a _base name_, used like `BASENAME-export-format-internal-name.extension`.

</details>

## 📕 Documentation

You can found some documentation:
 - here, on the project's README
 - via the Mediadeepa command line interface

The application provide a dynamic bash-completion script generated by:

```bash
java -jar target/mediadeepa-<VERSION>.jar --autocomplete
```

This document was check by Aspell with:

```bash
aspell check --lang=en_US --dont-backup --mode=markdown README.md
```

## 🎈 Road-map

Some changes have been planned, like:
 - Analyzing all audio streams, better MXF audio tracks and audio track management.
 - Better reports, (as PDF?) with embedded graphics.
 - Manage variable frame rate statistics (actually done, but need to be deeply checked to ensure the measure method is correct).
 - Work on live streams, instead of just regular files.
 - FFmpeg and FFprobe parallel executions.

And [many others](https://github.com/mediaexmachina/mediadeepa/issues?q=is%3Aopen+is%3Aissue+label%3Aenhancement), I hope !

### Known limitations for Mediadeepa

<details><summary>
You can found some information about what the application cannot do.
</summary>

 - It only support the first video, and the first founded audio stream of a file.
 - Audio mono and stereo only.
 - Some process take (long) time to do, like SITI and container analyzing, caused by poor FFmpeg/FFprobe performances with **this** filters.

</details>

## ❤️ Contributing and support

Free feel to add some time for this project like:
  - Send bug reports on [GitHub](https://github.com/mediaexmachina/mediadeepa/issues)
  - Help with the documentation
  - Propose pull requests
  - Or just take time to test the application and report the experience with it to me

Any contributions will be **highly appreciated** 🙏.

If you have any questions, feel free to reach out to me on
 - `@mediaexmachina` / `@hdsdi3g` on social media
 - Via any contact method listed on https://mexm.media

## 🙇 Author and copyright 

Copyright © Media ex Machina 2022+

## 📜 License

GNU General Public License v3

## 🌹 Acknowledgments

Mediadeepa would never have been possible without the help of these magnificent and amazing OSS projects:
 - [FFmpeg](https://FFmpeg.org/)
 - [Spring Boot](https://spring.io/projects/spring-boot)

🛠️ And the tech stack:
  - Java 17
  - Spring Boot 3
  - Picocli 4
  - My [`prodlib`](https://github.com/hdsdi3g/prodlib) and  [`medialib`](https://github.com/hdsdi3g/medialib) utility libs.
  - Maven (see `pom.xml` for more information)
  - Open CSV
  - Apache POI (poi-ooxml)
  - SQLite JDBC
  - Jackson
  - jFreechart
  - j2html

See `THIRD-PARTY.txt` file for more information on licenses and the full tech stack.
