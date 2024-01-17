## End-to-end automatic tests

In the project source repository, you will found some tools to end-to-ends (e2e) automatic tests, in order to check the application behavior with real video files.

> These tests are optional during the run of classical automatic tests, and only concerns dev. operations.

To run classical automatic tests, just run a `mvn test`.

To run e2e tests, you will need `ffmpeg` and `bash`:

 - Create video tests files on `.demo-media-files` with `bash create-demo-files.bash` (approx 230MB).
 - Optionally run `bash create-long-demo-file.bash` to create a big test video file.
 - Next, just run `mvn test`.

E2e tests take time. They will produce temp files in `target` directory (`e2e*` directories). A simple `mvn clean` wipe them, else the e2e scripts can reuse old generated files and don't loose some time.

E2e tests deeply checks all produced data from the application.
