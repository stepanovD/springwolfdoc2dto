package io.github.stepanovd.springwolf2dto.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SpringwolfDTOPluginTest {
    @Test
    void applyPlugin() {
        final Project project = ProjectBuilder.builder().build()
        project.getPlugins().apply(SpringwolfDTOPlugin.class)

        Assertions.assertTrue(project.getTasks().getNames().contains("generateDTO"))

        GenerateJsonSchemaJavaTask generateDTO = project.getTasks().getByName("generateDTO")

        Assertions.assertNull(null, generateDTO.getChannel().getOrNull())
    }
}
