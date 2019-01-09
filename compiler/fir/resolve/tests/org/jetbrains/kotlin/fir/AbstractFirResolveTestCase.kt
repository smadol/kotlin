/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirResolveTestCase : AbstractFirResolveWithSessionTestCase() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_NO_RUNTIME)
    }

    private fun createVirtualJavaFile(name: String, text: String): VirtualFile {
        var shortName = name.substring(name.lastIndexOf('/') + 1)
        shortName = shortName.substring(shortName.lastIndexOf('\\') + 1)
        val virtualFile = object : LightVirtualFile(shortName, JavaLanguage.INSTANCE, StringUtilRt.convertLineSeparators(text)) {
            override fun getPath(): String = "/$name"
        }

        virtualFile.charset = CharsetToolkit.UTF8_CHARSET
        return virtualFile
    }

    private fun doCreateAndProcessFir(ktFiles: List<KtFile>, javaFiles: List<File>): List<FirFile> {

        val scope = GlobalSearchScope.filesScope(
            project,
            ktFiles.mapNotNull { it.virtualFile } + javaFiles.map { createVirtualJavaFile(it.name, KotlinTestUtils.doLoadFile(it)) }
        )
        val session = createSession(scope)

        val builder = RawFirBuilder(session)

        val transformer = FirTotalResolveTransformer()
        return ktFiles.map { ktFile ->
            val firFile = builder.buildFirFile(ktFile)
            (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
            firFile
        }.also { firFile ->
            try {
                transformer.processFiles(firFile)
            } catch (e: Exception) {
                firFile.forEach { println(it.render()) }
                throw e
            }
        }
    }


    fun doTest(path: String) {
        val file = File(path)

        val allFiles = listOf(file) + file.parentFile.listFiles { sibling ->
            sibling.name.removePrefix(file.nameWithoutExtension)
                .removeSuffix(file.extension).removeSuffix("java").matches("\\.[0-9]+\\.".toRegex())
        }

        val ktFiles =
            allFiles.filter { it.extension != "java" }.map {
                val text = KotlinTestUtils.doLoadFile(it)
                it.name to text
            }.sortedBy { (_, text) ->
                KotlinTestUtils.parseDirectives(text)["analyzePriority"]?.toInt()
            }.map { (name, text) ->
                KotlinTestUtils.createFile(name, text, project)
            }

        val firFiles = doCreateAndProcessFir(ktFiles, allFiles.filter { it.extension == "java" })

        val firFileDump = StringBuilder().also { firFiles.first().accept(FirRenderer(it), null) }.toString()
        val expectedPath = path.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }
}