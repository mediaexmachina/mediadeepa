## Internal variables options

It's possible to configure additional options to define internal application variables.

To setup a variable (internally, an injected Spring Boot configuration, Mediadeepa will follow Spring Boot behaviors), you have several options.

### Use a YAML or Properties file

See [some documentation](https://www.baeldung.com/spring-yaml), [and this](https://www.baeldung.com/properties-with-spring).

Prepare your `application.yaml | application.property` file with the wanted options, and inject it to the app like:

```
mediadeepa --spring.config.location=application.yaml -i videofile.mov -c -f xlsx [...]
```

### Directly on command line

Simply with:

```
mediadeepa --mediadeepa.graphic-config.jpeg-compression-ratio=0.6 -i videofile.mov -c -f xlsx [...]
```

### Available options

You can refer to this full list:
