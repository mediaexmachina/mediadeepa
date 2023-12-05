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
