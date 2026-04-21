package com.embabel.agent.intellij

import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.nio.file.Path

/**
 * Test-only project service used by Driver to run the production inspection inside a live IDE.
 * This class is packaged in a separate plugin installed only for integration tests.
 */
@Service(Service.Level.PROJECT)
class InspectionIntegrationProbe(private val project: Project) {

    /**
     * Resolves [relativePath] against the opened test project, runs the production
     * [AchievesGoalVoidReturnInspection] for that file, and returns the rendered
     * problem descriptions. An empty list means the file could not be resolved or
     * the inspection did not report any problems.
     */
    fun findErrorDescriptions(relativePath: String): List<String> {
        
        val psiFile = loadPsiFile(relativePath) ?: return emptyList()
        loadDocument(psiFile) ?: return emptyList()

        return ReadAction.compute<List<String>, RuntimeException> {
            val inspectionManager = InspectionManager.getInstance(project)
            val inspection = LocalInspectionToolWrapper(AchievesGoalVoidReturnInspection())
            InspectionEngine.runInspectionOnFile(
                psiFile,
                inspection,
                inspectionManager.createNewGlobalContext(false),
            ).mapNotNull { it.descriptionTemplate }
        }
    }

    /**
     * Finds the PSI file inside the currently opened project for the relative path
     * used by the integration test.
     */
    private fun loadPsiFile(relativePath: String): PsiFile? = ReadAction.compute<PsiFile?, RuntimeException> {
        val basePath = project.basePath ?: return@compute null
        val nioPath = Path.of(basePath).resolve(relativePath).normalize()
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(nioPath) ?: return@compute null
        PsiManager.getInstance(project).findFile(virtualFile)
    }

    /**
     * Forces IntelliJ to create or load the backing document for the PSI file so
     * the file is fully ready for inspection in the running IDE.
     */
    private fun loadDocument(psiFile: PsiFile): Document? = ReadAction.compute<Document?, RuntimeException> {
        PsiDocumentManager.getInstance(project).getDocument(psiFile)
    }
}
