## Dependencies needed for run Mediadeepa

  - Java/JRE/JDK 21+
  - FFmpeg/FFprobe v5+ (v7+ highly recommended)

Declared on OS (Windows/Linux/macOS) PATH.

### Get Linux application packages

Download the last application release, as a Linux RPM or DEB package, or as an executable JAR (autonomous *fat* JAR file), downloaded directly from [GitHub releases page](https://github.com/mediaexmachina/mediadeepa/releases), and build at each releases.

Install/update with 

```bash
# DEB file on Debian/Ubuntu Linux distribs
sudo dpkg -i mediadeepa-<version>.deb

# RPM file on RHEL/CentOS Linux distribs
sudo rpm -U mediadeepa-<version>.rpm
```

Remove with `sudo dpkg -r mediadeepa` or `rpm -e mediadeepa`.

After, on Linux, run `mediadeepa [parameters]`, and `man mediadeepa` for the internal doc man page.

### Run simple JAR file

On Windows/macOS, just run `java -jar mediadeepa-<version>.jar [options]`.

And simply run the application with `java -jar mediadeepa-<version>.jar`.

Mediadeepa contain embedded help, displayed with the `-h` parameter.

You can set the command line parameters with `java -jar mediadeepa-<version>.jar [parameters]`.

### Make a Java executable JAR file

You can build yourself a JAR, with Git and Maven.

Run on Linux/WSL/macOS, after setup Git and Maven:

```bash
git clone https://github.com/mediaexmachina/mediadeepa.git
cd mediadeepa
mvn install -DskipTests
```

Build jar will be founded on `target` directory as `mediadeepa-<version>.jar`
