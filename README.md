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

This application will run FFmpeg on a source video/audio file to apply some filters, and generate analysis raw data (mostly high verbosely text/XML streams). They are parsed and reduced/converted/summarized them to some output formats by Mediadeepa.

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

## ⚡ Getting started

### 🔩 Dependencies needed for run Mediadeepa

  - Java/JRE/JDK 17+
  - FFmpeg/FFprobe v5+

Declared on OS (Windows/Linux/macOS) PATH.

Make the application jar file (autonomous *fat* jar) via Maven with

```bash
git clone https://github.com/mediaexmachina/mediadeepa.git
cd mediadeepa
mvn install -DskipTests
```

Build jar will found on `target` directory like `mediadeepa-<version>.jar`

And run the application simple with

```bash
java -jar mediadeepa-<version>.jar
```

Mediadeepa contain embedded help, displayed by default, or with `-h` parameter.

## 🛫 Usage

This application can run on three different modes:
 - [Process to export](#🌿-process-to-export)
 - [Process to extract](#🌿-process-to-extract)
 - [Import to export](#🌿-import-to-export)

### 🌿 Process to export

This is the classical mode.

Mediadeepa will drive FFmpeg to produce analysis data from your source file, and export the result a the end (actually, only in text files).

Use these options with this mode:

```
-i, --input FILE           Input (media) file to process
-c, --container            Do a container analysing (FFprobe streams)
-f, --format FORMAT_TYPE   Format to export datas
-e, --export DIRECTORY     Export datas to this directory
```

### 🌿 Process to extract

Sometimes, you don't need to process data during the analysis session.

So, Mediadeepa can just extract to raw text files all the gathered data from FFmpeg.

Use these options with this mode:

```
--extract-alavfi TEXT_FILE        Extract raw FFmpeg datas from LAVFI audio metadata filter
--extract-vlavfi TEXT_FILE        Extract raw FFmpeg datas from LAVFI video metadata filter
--extract-stderr TEXT_FILE        Extract raw FFmpeg datas from stderr
--extract-probeheaders XML_FILE   Extract XML FFprobe datas from container headers
--extract-probesummary TEXT_FILE  Extract simple FFprobe data summary from container headers
--extract-container XML_FILE      Extract XML FFprobe datas from container analyser
```

### 🌿 Import to export

And now, how do you get process analyst with this raw files ?

With the third mode: import, to load in Mediadeepa all gathered raw data files. Mediadeepa is **very tolerant** with this files, notably if they were not created by Mediadeepa (originally). **No one is mandatory.**

Use these options with this mode:

```
--import-lavfi TEXT_FILE        Import raw FFmpeg datas from LAVFI metadata filter
--import-stderr TEXT_FILE       Import raw FFmpeg datas from stderr filter
--import-probeheaders XML_FILE  Import XML FFprobe datas from container headers
--import-container XML_FILE     Import raw FFprobe datas from container analyser
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
[...]
```

You can use several exports formats, comma separated, like `-f txt,xml,json`.

_NB: exports formats like XML or JSON can produce very large files, which may take time to make, if want to use all filters/analysis scopes (up to 4 MB for a file less than 1 minute of media). It's not a problem for Mediadeepa, but it can be for you!_

The `-e` parameter set the target export directory to put the produced files.

The `--export-base-filename` set a _base name_, used like `BASENAME-export-format-internal-name.extension`.

</details>

## 📕 Documentation

You can found some documentation:
 - here, on the project's READ-ME
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
 - Analyzing all audio streams, better MXF/single audio track management
 - Produce numeric data in PNG/JPG graphics
   - Loudness
   - Audio stats
   - Bit-rate / Frame sizes
   - GOP Width / GOP size
   - Frame duration
   - Packet sizes
   - Media event (freeze, black, silence...)
   - Audio complexity / video complexity
   - ...
 - Create useful reports
   - HTML
   - PDF
   - DOCX
   - Open-Document text
 - Manage variable frame rate statistics
 - Work on live streams, instead of just regular files.

And [many others](https://github.com/mediaexmachina/mediadeepa/issues?q=is%3Aopen+is%3Aissue+label%3Aenhancement), I hope !

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

See `THIRD-PARTY.txt` file for more information on licenses and the full tech stack.
