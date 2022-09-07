# It's gradle plugin Springwolfdoc2dto

This plugin help you generate DTO classes based SpringWolf docs.

## Enable plugin

```groovy
plugins {
    id 'io.github.stepanovd.springwolf2dto' version '1.0.0-alpha'
}
```
## Configuration

### Parameters:

* __String url__ - URL to springwolf docs (default is *http://{domain}/springwolf/docs*)
* __String targetPackage__ - class package for generated classes
* __List<String> components__ - components for generate classes. Will be generated all components if the list is empty
* __String documentationTitle__ - title of service used in springwolf documentation. This title define key of documentation in json object.
* __Path targetDirectory__ - directory for save generated classes

### Set parameters:

```groovy
springWolfDoc2DTO.url = 'http://localhost:8080/springwolf/docs'
springWolfDoc2DTO.targetPackage = 'example.package'
springWolfDoc2DTO.documentationTitle = 'my-service'
```

## Run

```
./gradle -q generateDTO
```

