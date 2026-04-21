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

        myFixture.addClass(
            """
            package com.embabel.agent.api.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE)
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
            @Target(ElementType.METHOD)
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
        assertNoInspectionProblems()
    }

    /**
     * A plain {@code void} method in an {@code @Agent} class without {@code @AchievesGoal} is fine.
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
        assertNoInspectionProblems()
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
        assertNoInspectionProblems()
    }

    /**
     * An inner class inside an {@code @Agent} class is not itself an agent.
     * A {@code @AchievesGoal void} method in the inner class must not be flagged.
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
        assertNoInspectionProblems()
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
     */
    private fun assertNoInspectionProblems() {
        val problems = myFixture.doHighlighting().filter { it.description == AchievesGoalVoidReturnInspection.MESSAGE }
        assertTrue(
            "Expected no problems from AchievesGoalVoidReturnInspection but found ${problems.size}",
            problems.isEmpty(),
        )
    }
}
