package org.springwolf2dto;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.Set;

public record Configuration(
        @NonNull  String url,
        Set<String> resources,
        @NonNull Path outputJavaClassDirectory,
        @NonNull String packageName,
        @NonNull String documentationTitle
) {

}
