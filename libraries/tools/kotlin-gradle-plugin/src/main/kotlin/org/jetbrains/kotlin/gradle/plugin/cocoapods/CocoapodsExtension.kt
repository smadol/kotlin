/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

open class CocoapodsExtension(
    val project: Project,
    val kotlinExtension: KotlinMultiplatformExtension
) {
    @get:Input
    val version: String
        get() = project.version.toString()

    @Optional
    @Input
    var authors: String? = null

    @Optional
    @Input
    var license: String? = null

    @Optional
    @Input
    var summary: String? = null

    @Optional
    @Input
    var homepage: String? = null

    private val pods_ = mutableSetOf<CocoapodsDependency>()

    @get:Nested
    val pods: Set<CocoapodsDependency>
        get() = pods_

    // TODO: Interop conifguration.

    @JvmOverloads
    fun pod(name: String, version: String? = null, moduleName: String = name) {
        pods_.add(CocoapodsDependency(name, version, moduleName))
    }

    data class CocoapodsDependency(
        @Input val name: String,
        @Optional @Input val version: String?,
        @Input val moduleName: String
    )

}