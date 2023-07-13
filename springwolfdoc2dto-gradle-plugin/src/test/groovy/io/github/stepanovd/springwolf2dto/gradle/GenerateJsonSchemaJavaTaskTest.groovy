package io.github.stepanovd.springwolf2dto.gradle


import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GenerateJsonSchemaJavaTaskTest {
    @Test
    void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('generateSpringWolfDoc2DTO', type: GenerateJsonSchemaJavaTask)
        Assertions.assertTrue(task instanceof GenerateJsonSchemaJavaTask)
    }

//    @Test
//    void canRunTask() throws IOException {
//        // Setup the test build
//        File projectDir = new File("build/functionalTest");
//        Files.createDirectories(projectDir.toPath());
//        writeString(new File(projectDir, "settings.gradle"), "");
//        writeString(new File(projectDir, "build.gradle"), "plugins {" + "  id('io.github.stepanovd.springwolf2dto')" + "}");
//
//        // Run the build
//        GradleRunner runner = GradleRunner.create();
//        runner.forwardOutput();
//        runner.withPluginClasspath();
//        runner.withArguments("generateDTO");
//        runner.withProjectDir(projectDir);
//        BuildResult result = runner.build();
//
//        // Verify the result
//        Assertions.assertTrue(result.getOutput().contains("someoutput from the iwillfailyou task"));
//    }
//
//    private static void writeString(File file, String string) throws IOException {
//        try (Writer writer = new FileWriter(file)) {
//            writer.write(string);
//        }
//    }
}
