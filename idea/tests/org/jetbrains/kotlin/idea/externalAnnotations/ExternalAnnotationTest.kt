package org.jetbrains.kotlin.idea.externalAnnotations

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.JavaModuleExternalPaths
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

open class ExternalAnnotationTest : KotlinLightCodeInsightFixtureTestCase() {

    fun testDeprecated() {
        KotlinTestUtils.runTest(::doTest, TargetBackend.ANY, "idea/testData/externalAnnotations/deprecated.kt")
    }

    fun testNullable() {
        KotlinTestUtils.runTest(::doTest, TargetBackend.ANY, "idea/testData/externalAnnotations/nullable.kt")
    }

    override fun setUp() {
        super.setUp()
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = true
        addFile(classWithExternalAnnotatedMethods)
    }

    override fun tearDown() {
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = false
        super.tearDown()
    }

    private fun addFile(path: String) {
        val file = File(path)
        val root = LightPlatformTestCase.getSourceRoot()
        runWriteAction {
            val virtualFile = root.createChildData(null, file.name)
            virtualFile.getOutputStream(null).writer().use { it.write(FileUtil.loadFile(file)) }
        }
    }

    private fun doTest(kotlinFilePath: String) {
        myFixture.configureByFiles(kotlinFilePath, externalAnnotationsFile, classWithExternalAnnotatedMethods)
        myFixture.checkHighlighting()
    }

    override fun getProjectDescriptor() = object : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            super.configureModule(module, model)
            model.getModuleExtension(JavaModuleExternalPaths::class.java)
                    .setExternalAnnotationUrls(arrayOf(VfsUtilCore.pathToUrl(externalAnnotationsPath)))
        }
    }

    companion object {
        private const val externalAnnotationsPath = "idea/testData/externalAnnotations/annotations/"
        private const val classWithExternalAnnotatedMethods = "idea/testData/externalAnnotations/ClassWithExternalAnnotatedMethods.java"
        private const val externalAnnotationsFile = "$externalAnnotationsPath/annotations.xml"
    }
}
