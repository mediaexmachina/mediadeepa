## Limit the scope of analysis

By default, all analysis options and filters are activated in relation to your FFmpeg setup. Container analysis is still optional (via `-c`).

Not all options are necessarily useful for everyone and all the time (like [crop detect](https://ffmpeg.org/ffmpeg-filters.html#cropdetect)), the processing of certain filters can be very resource intensive (like [SITI](https://ffmpeg.org/ffmpeg-filters.html#siti-1)), and/or produce a large amount of data.

You can list all active filters with the `-o` option, directly loaded from your current FFmpeg setup.

> The description of each filter comes from the return of FFmpeg command.

And you can choose the analysis processing with these options:

 - `--filter-only FILTER` and `--filter-no FILTER`.
 - `--audio-no`, `--video-no` and `--media-no`.
 - `--container`

The `-e` parameter set the target export directory to put the produced files.

The `--export-base-filename` set a _base name_, used like `BASENAME-export-format-internal-name.extension`.
