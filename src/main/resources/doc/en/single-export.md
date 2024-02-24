## Single export option

With the help of `--single-export` option, you can choose and select an unique file to export, without the need to select an export format, or a directory.

The use of `--single-export` invalidate these options:  `-f/--format`,  `-e/--export` and `--export-base-filename`.

It can be used with a media or ZIP archive input file.

All filter options behaves the same way as full export: you should disable some analyzing options if you don't need it. Like `-vn` if you just want to export an audio related format.

Don't input (`-i`) more than one file.

### Syntax

```
--single-export <internal-file-name>:<outputfilename.ext>
```

With `<internal-file-name>`, the internal app file name "linked" to a export format. See below the full list.

With `<outputfilename.ext>`, the full path file to produce as result.

> Use `;` instead on `:` in Windows as path separator.

Example:

```
--single-export audio-loudness:/home/me/lufs.png
```

Will produce an `audio-loudness` graphic file on `/home/me/lufs.png` file.

For information, `graphic` export format files has *no extension*. By default, it export **PNG** files. Change this with `--graphic-jpg`.

Here the full available internal files that you can use (CSV FR format can't be selected):
