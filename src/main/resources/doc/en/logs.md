### Logging

You can manage output logs with specific options, like `--verbose`, `-q` and `--log`.

This application use internally [Logback](https://logback.qos.ch/). The actual and default configuration XML file can be found on source code in `src/main/resources/logback.xml`.

To inject a new logback configuration file, add in application command line:

```
-Dlogging.config="path/to/new/logback.xml"
```
