## Search path binaries

Mediadeepa can search on several paths to found `ffmpeg`/`ffmpeg.exe` and `ffprobe`/`ffprobe.exe` (sorted by search order):
 - directly declared on command line by `-Dexecfinder.searchdir=c:\path1;c:\path2\subpath` on Windows or `-Dexecfinder.searchdir=/path1:/path2/subpath` on Posix
 - on `$HOME` directory
 - on `$HOME/bin` directory, if exists
 - on `$HOME/App/bin` directory, if exists
 - on any classpath directory declared, if exists
 - on the global `PATH` environment variable

Either on Linux/Posix and Windows.

You can inject other binary names (other than `ffmpeg`/`ffprobe`) with: `mediadeepa.ffmpegExecName` and `mediadeepa.ffprobeExecName` configuration keys. `.exe` on Windows will be added/removed as needed by the application.

In summary, if FFmpeg/FFprobe is runnable from anywhere on your host (`PATH`), you'll have nothing to do.
