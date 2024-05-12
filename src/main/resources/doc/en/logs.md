### Logging

You can manage output logs with specific options, like `--verbose`, `-q` and `--log`.

This application use internally [Logback](https://logback.qos.ch/). The actual and default configuration XML file can be found on source code in `src/main/resources/logback.xml`.

To inject a new logback configuration file, add in application command line:

```
-Dlogging.config="path/to/new/logback.xml"
```

For information, the use of `--single-export` option to `-` (std out) will cut all std out log messages, but you will stay able to send log messages to text file via `--log` option.
