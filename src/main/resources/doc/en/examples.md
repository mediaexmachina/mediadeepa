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

## Import or Process to extract

Just:

```
mediadeepa -i videofile.mov --extract analysing-archive.zip
```

You can setup FFmpeg, like with import, like:

```
mediadeepa -i videofile.mov -c -an --extract analysing-archive.zip
```

Extracted (archive) ZIP file can be loaded simply by `-i`:

```
mediadeepa -i analysing-archive.zip -f report -f graphic -e .
```

## Multiple Import or Process

Add `-i` options to works with multiple files, like:

```
mediadeepa -i analysing-archive.zip -i videofile.mov -i anotherfile.wav -f report -f graphic -e .
```

You can mix archive zip files and media files, but beware to not *import* with *extract* (zip to zip) or use single output file mode (`--single-export`).

## Directory scan to input files

With the same restrictions as _Multiple Import or Process_, you can use a directory with `-i` parameter.

```
mediadeepa -i /some/directory -i /some/another/directory -f report -f graphic -e .
```

All _non hidden_ founded files, not recursively (ignore the sub directories) will be used. You should use include/exclude parameter to manage the file selection criteria.

Use:

```
mediadeepa -i /some/directory --recursive --exclude-path never-this --include-ext ".mkv" -f report -f graphic -e .
```

To

 - scan recursively `/some/directory` directory
 - with the `/some/directory/never-this/*` directory ignored
 - only for MKV files

More options are available.

## Realities directory scan to input files

With the same options and restrictions as _Directory scan to input files_, just add `--scan 10` to scan every 10 seconds all provided directories (simple `-i` files will be processed on application starts), like:

```
mediadeepa -i /some/directory --scan 10 -f report -f graphic -e .
```

Stop the scans with a key-press, or just with `CTRL+C`.

## Load files to process from a text file

With the `-il`, as *input list* option:

```
mediadeepa -if my-medias.txt -f report -f graphic -e .
```

And the `my-medias.txt` file can just contain:

```
analysing-archive.zip
videofile.mov
anotherfile.wav
```

 * Any space lines are ignored.
 * Charset load respect the current OS session.
 * You can use Windows and Linux new lines symbols (and you can mix them).
 * You can accumulate multiple `-i` and `-il` options, with the same limits as *Multiple Import or Process*.
 * Before starts the imports and processing, the application will check and throw an error if a file is missing (in `-i`, `-il`, and in the lists itself).
