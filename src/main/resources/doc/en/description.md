Audio/video medias and streams deep analyzer in Java with FFmpeg as back-end: extract/process technical information from audio/videos files/streams.

**This application is currently in alpha version, and should not be ready for production**

Known limitations for Mediadeepa:

 - It only support the first video, and the first founded audio stream of a file.
 - Audio mono and stereo only.
 - Some process take (long) time to do, like SITI and container analyzing, caused by poor FFmpeg/FFprobe performances with **this** filters.
