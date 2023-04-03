package io.github.stepanovd.springwolf2dto.gradle


import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GenerateJsonSchemaJavaTaskTest {
    @Test
    void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('generateSpringWolfDoc2DTO', type: GenerateJsonSchemaJavaTask)
        Assertions.assertTrue(task instanceof GenerateJsonSchemaJavaTask)
    }
}
