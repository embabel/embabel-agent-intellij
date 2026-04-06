package com.embabel.agent.intellij

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class ActionImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement): Boolean =
        element is PsiMethod && FRAMEWORK_ANNOTATIONS.any { fqn ->
            element.hasAnnotation(fqn)
        }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false

    companion object {
        private val FRAMEWORK_ANNOTATIONS = setOf(
            "com.embabel.agent.api.annotation.Action",
            "com.embabel.agent.api.annotation.Condition",
            "com.embabel.agent.api.annotation.Cost",
        )
    }
}
