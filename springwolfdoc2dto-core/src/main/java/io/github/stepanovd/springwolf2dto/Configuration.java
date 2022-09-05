/**
 * Copyright © 2010-2014 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.stepanovd.springwolf2dto;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.Set;

/**
 *
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
public record Configuration(
        @NonNull  String url,
        Set<String> resources,
        @NonNull Path outputJavaClassDirectory,
        @NonNull String packageName,
        @NonNull String documentationTitle
) {

}
