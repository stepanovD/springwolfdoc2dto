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

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the plugin's tasks.
 *
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
class SpringwolfDTOPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        def extension = project.extensions.create('springWolfDoc2DTO', SpringwolfDTOExtension)

        project.getTasks().register(
                'generateSpringWolfDoc2DTO',
                GenerateJsonSchemaJavaTask,
                { task ->
                    task.getURL().set(extension.url)
                    task.getDocumentationTitle().set(extension.documentationTitle)
                    task.getTargetPackage().set(extension.targetPackage)
                    task.getTargetDirectory().set(extension.targetDirectory)
                }
        )

//        project.task('generateDtoTest') {
//            doLast {
//                println "${extension.url.get()} to ${extension.targetPackage.get()}"
//            }
//        }
//        project.task('generateDto') {
//            doLast {
//                Configuration configuration = new Configuration(
//                        extension.url.get(),
//                        null,
//                        extension.targetDirectory.get(),
//                        extension.targetPackage.get(),
//                        extension.documentationTitle.get()
//                )
//
//                Connector connector = new HttpConnector()
//
//                PojoGenerator.convertJsonToJavaClass(configuration, connector)
//
//                println "${extension.url.get()} to ${extension.targetPackage.get()}"
//            }
//        }


    }
}