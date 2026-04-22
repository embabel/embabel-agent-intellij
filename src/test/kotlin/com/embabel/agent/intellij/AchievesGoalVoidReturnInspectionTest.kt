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

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/**
 * Tests for [AchievesGoalVoidReturnInspection].
 *
 * [setUp] registers minimal stub {@code @Agent} and {@code @AchievesGoal} annotations
 * (the plugin has no compile-time dependency on the Embabel library) and enables the inspection.
 * Each test then configures a Java source file and verifies that the inspection either reports
 * an error (positive case) or stays silent (negative cases).
 */
class AchievesGoalVoidReturnInspectionTest : LightJavaCodeInsightFixtureTestCase() {

    /**
     * Registers stub Embabel annotations in the test project and enables the inspection
     * so that subsequent tests can trigger highlighting.
     */
    override fun setUp() {

        super.setUp()

        // Stubs are intentionally permissive (no @Target) so that test fixtures
        // placing them on any element kind remain valid Java.
        myFixture.addClass(
            """
            package com.embabel.agent.api.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Agent {
                String description();
            }
        """.trimIndent()
        )

        myFixture.addClass(
            """
            package com.embabel.agent.api.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            public @interface AchievesGoal {
                String description();
            }
        """.trimIndent()
        )

        myFixture.enableInspections(AchievesGoalVoidReturnInspection::class.java)
    }

    /**
     * Positive case: a {@code void} method annotated with {@code @AchievesGoal}
     * inside an {@code @Agent} class must be flagged as an error.
     */
    fun testVoidReturnInAgentClassReportsError() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                @AchievesGoal(description = "goal")
                public void <error descr="${AchievesGoalVoidReturnInspection.MESSAGE}">badMethod</error>() {}
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * A non-void return type is valid — no error expected.
     */
    fun testNonVoidReturnInAgentClassNoError() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                @AchievesGoal(description = "goal")
                public String goodMethod() { return "ok"; }
            }
        """.trimIndent()
        )
        assertNoInspectionProblems()
    }

    /**
     * {@code @AchievesGoal void} outside an {@code @Agent} class should not be flagged.
     * Uses {@code checkHighlighting()} (not the filtered helper) so any unexpected error is caught.
     */
    fun testVoidReturnWithoutAgentAnnotationNoError() {

        myFixture.configureByText(
            "NotAnAgent.java", """
            import com.embabel.agent.api.annotation.AchievesGoal;

            public class NotAnAgent {
                @AchievesGoal(description = "goal")
                public void someMethod() {}
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * A plain {@code void} method in an {@code @Agent} class without {@code @AchievesGoal} is fine.
     * Uses {@code checkHighlighting()} (not the filtered helper) so any unexpected error is caught.
     */
    fun testVoidReturnWithoutAchievesGoalNoError() {
        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;

            @Agent(description = "test")
            public class TestAgent {
                public void plainMethod() {}
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * Returning {@code Object} (non-void) is valid — no error expected.
     */
    fun testNonVoidObjectReturnNoError() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                @AchievesGoal(description = "goal")
                public Object objectMethod() { return null; }
            }
        """.trimIndent()
        )
        assertNoInspectionProblems()
    }

    /**
     * Multiple {@code @AchievesGoal void} methods in the same {@code @Agent} class
     * must each be flagged independently.
     */
    fun testMultipleVoidMethodsEachFlagged() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                @AchievesGoal(description = "goal A")
                public void <error descr="${AchievesGoalVoidReturnInspection.MESSAGE}">goalA</error>() {}

                @AchievesGoal(description = "goal B")
                public void <error descr="${AchievesGoalVoidReturnInspection.MESSAGE}">goalB</error>() {}
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * Constructors have a {@code null} return type (not {@code void}).
     * The inspection must silently skip them even if annotated with {@code @AchievesGoal}.
     * Uses {@code checkHighlighting()} (not the filtered helper) so any unexpected error is caught.
     */
    fun testConstructorWithAchievesGoalNoError() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                @AchievesGoal(description = "goal")
                public TestAgent() {}
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * An inner class inside an {@code @Agent} class is not itself an agent.
     * A {@code @AchievesGoal void} method in the inner class must not be flagged.
     * Uses {@code checkHighlighting()} (not the filtered helper) so any unexpected error is caught.
     */
    fun testVoidMethodInInnerClassOfAgentNoError() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                public class Inner {
                    @AchievesGoal(description = "goal")
                    public void innerMethod() {}
                }
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * Annotations referenced by fully qualified name (no import) must still be recognized.
     */
    fun testFullyQualifiedAnnotationsReportsError() {

        myFixture.configureByText(
            "TestAgent.java", """
            @com.embabel.agent.api.annotation.Agent(description = "test")
            public class TestAgent {
                @com.embabel.agent.api.annotation.AchievesGoal(description = "goal")
                public void <error descr="${AchievesGoalVoidReturnInspection.MESSAGE}">badMethod</error>() {}
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    /**
     * Verifies that the inspection offers suppression actions (the standard {@code @SuppressWarnings}
     * and {@code //noinspection} mechanisms). Actual suppression filtering requires a full JDK and
     * the complete Java inspection suppressor pipeline, which the light test fixture does not provide.
     * This test verifies the contract by checking that suppression intention actions are available
     * at the error site.
     */
    fun testSuppressionActionsAvailable() {

        myFixture.configureByText(
            "TestAgent.java", """
            import com.embabel.agent.api.annotation.Agent;
            import com.embabel.agent.api.annotation.AchievesGoal;

            @Agent(description = "test")
            public class TestAgent {
                @AchievesGoal(description = "goal")
                public void bad<caret>Method() {}
            }
        """.trimIndent()
        )
        val intentions = myFixture.availableIntentions
        val hasSuppression = intentions.any {
            it.familyName.contains("Suppress") || it.text.contains("Suppress")
        }
        assertTrue("Suppression actions should be available at the error site", hasSuppression)
    }

    /**
     * Verifies that the HTML description file is on the classpath so the inspection settings panel is not blank.
     */
    fun testInspectionHasDescription() {

        val inspection = AchievesGoalVoidReturnInspection()
        val descriptionPath = "/inspectionDescriptions/${inspection.shortName}.html"
        val url = inspection.javaClass.getResource(descriptionPath)
            ?: javaClass.getResource(descriptionPath)
            ?: Thread.currentThread().contextClassLoader?.getResource(descriptionPath.removePrefix("/"))
        assertNotNull("$descriptionPath must be on the classpath", url)
    }

    /**
     * Asserts that no highlights matching this inspection's message exist in the current file.
     *
     * Use this only when the fixture contains types that the light test fixture cannot resolve
     * (e.g. {@code String}, {@code Object}) — those produce unrelated "Cannot resolve symbol" errors
     * that make {@code checkHighlighting()} fail. For fixtures that use only {@code void} returns
     * and custom annotations, prefer {@code checkHighlighting()} instead so that any unexpected
     * error in the fixture is caught.
     */
    private fun assertNoInspectionProblems() {
        val problems = myFixture.doHighlighting().filter { it.description == AchievesGoalVoidReturnInspection.MESSAGE }
        assertTrue(
            "Expected no problems from AchievesGoalVoidReturnInspection but found ${problems.size}",
            problems.isEmpty(),
        )
    }
}
