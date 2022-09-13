# It's gradle plugin Springwolfdoc2dto

This plugin help you generate DTO classes based SpringWolf docs.

## Enable plugin

```groovy
plugins {
    id 'io.github.stepanovd.springwolf2dto' version '1.0.1-alpha'
}
```
## Configuration

### Parameters:

* __String url__ - URL to springwolf docs (default is *http://{domain}/springwolf/docs*)
* __String targetPackage__ - class package for generated classes
* __String documentationTitle__ - title of service used in springwolf documentation. This title define key of documentation in json object.
* __Path targetDirectory__ - directory for save generated classes

### Set parameters:

```groovy
springWolfDoc2DTO{
    url = 'http://localhost:8080/springwolf/docs'
    targetPackage = 'example.package'
    documentationTitle = 'my-service'
    targetDirectory = project.layout.getBuildDirectory().dir("generated-sources")
}
```

## Run

```
./gradle -q generateDTO
```

