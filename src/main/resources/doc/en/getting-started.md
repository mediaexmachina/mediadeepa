### 🔩 Dependencies needed for run Mediadeepa

  - Java/JRE/JDK 17+
  - FFmpeg/FFprobe v5+

Declared on OS (Windows/Linux/macOS) PATH.

Download the last application release, as an executable JAR (autonomous *fat* JAR file), downloaded directly from [GitHub releases page](https://github.com/mediaexmachina/mediadeepa/releases), and build at each releases.

<details>
<summary>Or, you can build yourself this JAR, with Git and Maven.</summary>

Run on Linux/WSL/macOS, after setup Git and Maven:

```bash
git clone https://github.com/mediaexmachina/mediadeepa.git
cd mediadeepa
mvn install -DskipTests
```

Build jar will be founded on `target` directory as `mediadeepa-<version>.jar`

</details>

And simply run the application with

```bash
java -jar mediadeepa-<version>.jar
```

Mediadeepa contain embedded help, displayed with the `-h` parameter.

You can set the command line parameters with `java -jar mediadeepa-<version>.jar [parameters]`.

> Example: `java -jar mediadeepa-<version>.jar -h`
