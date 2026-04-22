/*
 * Copyright 2024-2026 Embabel Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.agent.intellij

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

/**
 * End-to-end regression test for the live IntelliJ inspection wiring.
 *
 * The production plugin is installed exactly as a user would install it. A second,
 * test-only plugin is installed alongside it to expose a small Driver-accessible
 * project service. That service lets the test ask the running IDE to execute the
 * real inspection against a file in the sample project and return the reported
 * error descriptions.
 */
class AchievesGoalVoidReturnIntegrationTest {

    private data class Problem(val line: String, val description: String)

    /**
     * Opens the sample project in a real IntelliJ IDEA instance and verifies that
     * the production inspection reports the expected error for a `void`-returning
     * `@AchievesGoal` method.
     */
    @Test
    fun flagsVoidReturningAchievesGoalMethodInLiveIde() {
        val testProject = Path.of(
            "src",
            "integrationTest",
            "resources",
            "test-projects",
            "achieves-goal-inspection",
        )
        val agentFile = testProject.resolve("src/demo/TestAgent.java")
        val badMethodLine = findLineNumber(agentFile, "public void badMethod() {}")
        val goodMethodLine = findLineNumber(agentFile, "public String goodMethod() {")

        Starter.newContext(
            testName = "achievesGoalVoidReturnInspection",
            testCase = TestCase(IdeProductProvider.IC, LocalProjectInfo(testProject))
                .withVersion("2025.2"),
        ).apply {
            val pluginConfigurator = PluginConfigurator(this)
            pluginConfigurator.installPluginFromPath(File(System.getProperty("path.to.build.plugin")).toPath())
            pluginConfigurator.installPluginFromPath(File(System.getProperty("path.to.integration.test.plugin")).toPath())
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForProjectOpen()
            waitForIndicators(2.minutes)

            val project = singleProject()
            val problems = service<InspectionIntegrationProbeRemote>(project)
                .findProblems("src/demo/TestAgent.java")
                .map { encodedProblem ->
                    val separatorIndex = encodedProblem.indexOf('|')
                    Problem(
                        line = encodedProblem.substring(0, separatorIndex),
                        description = encodedProblem.substring(separatorIndex + 1),
                    )
                }
            val matchingProblems = problems.filter {
                it.description == AchievesGoalVoidReturnInspection.MESSAGE
            }

            assertTrue(
                matchingProblems.isNotEmpty(),
                "Expected live IDE highlighting to contain the AchievesGoal void-return error, but got $problems",
            )
            assertEquals(
                1,
                matchingProblems.size,
                "Expected exactly one live IDE inspection error in TestAgent.java",
            )
            assertTrue(
                matchingProblems.any { it.line == badMethodLine },
                "Expected badMethod on line $badMethodLine to be flagged, but got $matchingProblems",
            )
            assertTrue(
                matchingProblems.none { it.line == goodMethodLine },
                "Expected goodMethod on line $goodMethodLine to remain unflagged, but got $matchingProblems",
            )
        }
    }

    private fun findLineNumber(file: Path, needle: String): String =
        Files.readAllLines(file)
            .indexOfFirst { it.contains(needle) }
            .let { index ->
                check(index >= 0) { "Could not find '$needle' in $file" }
                (index + 1).toString()
            }
}

/**
 * Driver-side view of the test-only project service registered by the auxiliary
 * integration-test plugin.
 */
@Remote(
    value = "com.embabel.agent.intellij.InspectionIntegrationProbe",
    plugin = "com.embabel.agent.intellij-plugin.integration-test",
)
interface InspectionIntegrationProbeRemote {
    /**
     * Runs the inspection in the live IDE for the file at [relativePath] and
     * returns problem metadata for any problems that were reported.
     */
    fun findProblems(relativePath: String): List<String>
}
