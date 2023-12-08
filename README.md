<!-- AUTOGENERATED DOCUMENT BY MEDIADEEPA-->
<!-- DO NOT EDIT, SOURCE ARE LOCATED ON src/main/resources/doc/en-->

<!-- GENERATED BY media.mexm.mediadeepa.components.DocumentationExporter-->

<h1 id="mediadeepanalysis">Media Deep Analysis</h1>

Audio/video medias and streams deep analyzer in Java with FFmpeg as back-end: extract/process technical information from audio/videos files/streams.

**This application is currently in alpha version, and should not be ready for production**

> [🚩 About](#about)\
> [🏪 Features](#features)\
> [⚡ Getting started](#gettingstarted)\
> [🛫 Examples](#examples)\
> [📕 Documentation, contributing and support](#documentationcontributingandsupport)\
> [🌹 Acknowledgments](#acknowledgments)

[![CodeQL](https://github.com/mediaexmachina/mediadeepa/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/mediaexmachina/mediadeepa/actions/workflows/codeql-analysis.yml)
[![Java CI with Maven](https://github.com/mediaexmachina/mediadeepa/actions/workflows/maven-package.yml/badge.svg)](https://github.com/mediaexmachina/mediadeepa/actions/workflows/maven-package.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mediaexmachina_mediadeepa&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=mediaexmachina_mediadeepa)

<h2 id="about">🚩 About</h2>

This application will run FFmpeg on a source video/audio file to apply some filters, and generate analysis raw data (mostly high verbosely text/XML streams). They are parsed and reduced/converted/drawn/summarized them to some output formats by Mediadeepa.

> Mediadeepa is a command line standalone application (no GUI, no specific setup).

This application is licensed with the GNU General Public License v3.

<h2 id="features">🏪 Features</h2>

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

This application can run on three different modes:
 - **Process to export**: this is the classical mode. Mediadeepa will drive FFmpeg to produce analysis data from your source file, and export the result a the end.
 - **Process to extract**: sometimes, you don't need to process data during the analysis session. So, Mediadeepa can just extract to raw text/xml files (zipped in one archive file) all the gathered data from FFmpeg.
 - **Import to export**: to load in Mediadeepa all gathered raw data files. Mediadeepa is **very tolerant** with the zip content, notably if they were not created by Mediadeepa (originally). **No one is mandatory in zip.**

## Known limitations for Mediadeepa

 - It only support the first video, and the first founded audio stream of a file.
 - Audio mono and stereo only.
 - Some process take (long) time to do, like SITI and container analyzing, caused by poor FFmpeg/FFprobe performances with **this** filters.

<h2 id="gettingstarted">⚡ Getting started</h2>

## Dependencies needed for run Mediadeepa

  - Java/JRE/JDK 17+
  - FFmpeg/FFprobe v5+

Declared on OS (Windows/Linux/macOS) PATH.

Download the last application release, as an executable JAR (autonomous *fat* JAR file), downloaded directly from [GitHub releases page](https://github.com/mediaexmachina/mediadeepa/releases), and build at each releases.

Or, you can build yourself this JAR, with Git and Maven.

Run on Linux/WSL/macOS, after setup Git and Maven:

```bash
git clone https://github.com/mediaexmachina/mediadeepa.git
cd mediadeepa
mvn install -DskipTests
```

Build jar will be founded on `target` directory as `mediadeepa-0.0.21.jar`

And simply run the application with

```bash
java -jar mediadeepa-0.0.21.jar
```

Mediadeepa contain embedded help, displayed with the `-h` parameter.

You can set the command line parameters with `java -jar mediadeepa-0.0.21.jar [parameters]`.

> Example: `java -jar mediadeepa-0.0.21.jar -h`

<h2 id="examples">🛫 Examples</h2>

## Process to export

Export to the current directory the analysis report for the file `videofile.mov`:

```
mediadeepa -i videofile.mov -f report -e .
```

Export to my `Download` directory the analysis result, as MS Excel and graphic files, the media file `videofile.mov`, only for audio and media container:

```
mediadeepa -i videofile.mov -c -f xlsx -f graphic -vn -e $HOME/Downloads
```

All available **Export formats type** are listed by:

```
mediadeepa -o
```

## Process to extract

Just:

```
mediadeepa -i videofile.mov --extract analysing-archive.zip
```

You can setup FFmpeg, like with import, like:

```
mediadeepa -i videofile.mov -c -an --extract analysing-archive.zip
```

## Import to export

Replace `-i` option by `--import`:

```
mediadeepa --import analysing-archive.zip -f report -f graphic -e .
```

You can read the [FFmpeg filter documentation](https://ffmpeg.org/ffmpeg-filters.html) to know the behavior for each used filters, and the kind of returned values.

<h2 id="documentationcontributingandsupport">📕 Documentation, contributing and support</h2>

You can found some documentation:
 - On the project's README on [GitHub](https://github.com/mediaexmachina/mediadeepa).
 - On the Mediadeepa website [https://gh.mexm.media/](https://gh.mexm.media/)
 - On the Mediadeepa command line interface.
 - On the integrated app man page.

Send bug reports on [GitHub project page](https://github.com/mediaexmachina/mediadeepa/issues)
 - Help with the documentation.
 - Propose pull requests.
 - Or just take time to test the application and report the experience.

If you have any questions, feel free to reach out via any contact method listed on [https://mexm.media](https://mexm.media).

<h2 id="acknowledgments">🌹 Acknowledgments</h2>

Mediadeepa would never have been possible without the help of these magnificent and amazing OSS projects:
 - [FFmpeg](https://FFmpeg.org/)
 - [Spring Boot](https://spring.io/projects/spring-boot)

And the tech stack:
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

