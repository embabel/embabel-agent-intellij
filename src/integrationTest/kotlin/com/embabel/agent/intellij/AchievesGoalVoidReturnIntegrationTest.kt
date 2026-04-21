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
            val descriptions = service<InspectionIntegrationProbeRemote>(project)
                .findErrorDescriptions("src/demo/TestAgent.java")

            assertTrue(
                descriptions.contains(AchievesGoalVoidReturnInspection.MESSAGE),
                "Expected live IDE highlighting to contain the AchievesGoal void-return error, but got $descriptions",
            )
            assertEquals(
                1,
                descriptions.count { it == AchievesGoalVoidReturnInspection.MESSAGE },
                "Expected exactly one live IDE inspection error in TestAgent.java",
            )
        }
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
     * returns the descriptions of any problems that were reported.
     */
    fun findErrorDescriptions(relativePath: String): List<String>
}
