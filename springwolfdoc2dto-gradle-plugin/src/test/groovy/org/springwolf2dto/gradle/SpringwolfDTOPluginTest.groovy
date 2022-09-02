package org.springwolf2dto.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SpringwolfDTOPluginTest {
    @Test
    void springwolfDTOPluginAddsGenerateJsonSchemaJavaTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'org.springwolf2dto'
        println 'springwolfDTOPluginAddsGenerateJsonSchemaJavaTaskToProject'
    }
}
