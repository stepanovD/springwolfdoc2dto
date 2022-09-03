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
package io.github.stepanovd.springwolf2dto.gradle

import io.github.stepanovd.springwolf2dto.HttpConnector
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import io.github.stepanovd.springwolf2dto.Configuration
import io.github.stepanovd.springwolf2dto.Connector
import io.github.stepanovd.springwolf2dto.PojoGenerator

import java.nio.file.Files
import java.nio.file.Path

/**
 * A task that performs code generation.
 *
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
abstract class GenerateJsonSchemaJavaTask extends DefaultTask {
    @Input
    abstract Property<String> getURL()

    @Input
    abstract Property<String> getTargetPackage()

    @Input
    abstract Property<String> getDocumentationTitle()

    @Input
    abstract Property<Path> getTargetDirectory()

    GenerateJsonSchemaJavaTask() {
        description = 'Generates Java classes from a json schema.'
        group = 'Build'

    }

//  def configureJava() {
//    project.sourceSets.main.java.srcDirs += [ configuration.targetDirectory ]
//    dependsOn(project.tasks.processResources)
//    project.tasks.compileJava.dependsOn(this)
//  }

    @TaskAction
    def generate() {
        String url = getURL().get() ?: "http://localhost:8080/springwolf/docs"
        String targetPackage = getTargetPackage().get() ?: ""
        String documentationTitle = getDocumentationTitle().get() ?: "service"
        Path targetDirectory = getTargetDirectory().get() ?: project.file("${project.buildDir}/generated-sources/generated-dto").toPath()

        Configuration configuration = new Configuration(url, null, targetDirectory, targetPackage, documentationTitle)

//        if (project.plugins.hasPlugin('java')) {
//            configureJava()
//        } else {
//            throw new GradleException('generateJsonSchema: Java plugin is required')
//        }
        outputs.dir configuration.outputJavaClassDirectory()

        inputs.property("configuration", configuration.toString())

        logger.info 'Using this configuration:\n{}', configuration

        Connector connector = new HttpConnector()

        Files.createDirectories(configuration.outputJavaClassDirectory())

        PojoGenerator.convertJsonToJavaClass(configuration, connector)

        println "${configuration.url()} to ${configuration.packageName()}"
    }
}