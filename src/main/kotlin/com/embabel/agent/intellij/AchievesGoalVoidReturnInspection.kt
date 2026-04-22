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

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes

/**
 * Reports an error when a method annotated with {@code @AchievesGoal} in an {@code @Agent} class
 * returns {@code void}.
 *
 * In the Embabel framework the return type of {@code @AchievesGoal} method defines the goal's
 * output type. A {@code void} return is semantically invalid and will produce broken agent plans
 * at runtime, so this inspection flags it as an error in the editor.
 */
class AchievesGoalVoidReturnInspection : AbstractBaseJavaLocalInspectionTool() {

    /**
     * Fallback description shown in Settings > Inspections if the HTML description file
     * (inspectionDescriptions/AchievesGoalVoidReturn.html) cannot be loaded.
     */
    override fun getStaticDescription(): String =
        "Reports methods annotated with @AchievesGoal in an @Agent class that return void. " +
            "In the Embabel framework, the return type of an @AchievesGoal method defines " +
            "the goal's output type. A void return is semantically invalid and will produce " +
            "broken agent plans at runtime. " +
            "Change the return type to a domain object that represents the goal output."

    /**
     * Returns a visitor that checks each Java method for the error condition.
     * The checks are ordered cheapest-first to bail out early on most methods:
     * 1. Method has @AchievesGoal — skips the vast majority of methods immediately.
     * 2. Return type is void — no problem if the method already returns a real type.
     * 3. Containing class has @Agent — the rule only applies inside agent classes.
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val voidType = PsiTypes.voidType()

        return object : JavaElementVisitor() {

            override fun visitMethod(method: PsiMethod) {

                if (!method.hasAnnotation(ACHIEVES_GOAL_FQN)) return

                // Constructors have a null returnType — skip them
                val returnType = method.returnType ?: return
                if (returnType != voidType) return

                val containingClass = method.containingClass ?: return
                if (!containingClass.hasAnnotation(AGENT_FQN)) return

                holder.registerProblem(
                    method.nameIdentifier ?: method, MESSAGE, ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
    }

    companion object {
        const val ACHIEVES_GOAL_FQN = "com.embabel.agent.api.annotation.AchievesGoal"
        const val AGENT_FQN = "com.embabel.agent.api.annotation.Agent"
        const val MESSAGE = "@AchievesGoal method must not return void - the return type is used as the goal output type"
    }
}
