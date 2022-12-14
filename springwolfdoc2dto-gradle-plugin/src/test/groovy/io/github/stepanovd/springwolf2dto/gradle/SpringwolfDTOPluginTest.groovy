package io.github.stepanovd.springwolf2dto.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class SpringwolfDTOPluginTest {
    @Test
    void springwolfDTOPluginAddsGenerateJsonSchemaJavaTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'io.github.stepanovd.springwolf2dto'
        println 'springwolfDTOPluginAddsGenerateJsonSchemaJavaTaskToProject'
    }
}
