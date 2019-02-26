/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import java.io.File

/**
 * The task generates a podspec file which allows a user to
 * integrate a Kotlin/Native framework into a Cocoapods project.
 */
open class PodspecTask: DefaultTask() {

    @OutputFile
    val outputFile: File = project.projectDir.resolve("${project.name}.podspec")

    @Nested
    lateinit var settings: CocoapodsExtension

    // TODO: Framework name customization.
    // TODO: Update compiler version.
    @TaskAction
    fun generate() {
        val frameworkDir = project.cocoapodsBuildDirs.framework.relativeTo(outputFile.parentFile).path
        val dependencies = settings.pods.map { (name, version, _) ->
            "|spec.dependency $name${version.let { ", $it" }}"
        }.joinToString(separator = "\n")
        val specName = project.name.replace('-', '_')

        // TODO: Do we need this framework_dir?
        outputFile.writeText("""
            |Pod::Spec.new do |spec|
            |    framework_dir = "${'$'}{PODS_TARGET_SRCROOT}/$frameworkDir"
            |
            |    spec.name                     = '$specName'
            |    spec.version                  = '${settings.version}'
            |    spec.homepage                 = '${settings.homepage.orEmpty()}'
            |    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
            |    spec.authors                  = '${settings.authors.orEmpty()}'
            |    spec.license                  = '${settings.license.orEmpty()}'
            |    spec.summary                  = '${settings.summary.orEmpty()}'
            |
            |    spec.static_framework         = true
            |    spec.vendored_frameworks      = "$frameworkDir/#{spec.name}.framework"
            |    spec.libraries                = "c++"
            |    spec.module_name              = "#{spec.name}_umbrella"
            |
            $dependencies
            |
            |    spec.pod_target_xcconfig = {
            |        'KOTLIN_TARGET[sdk=iphonesimulator*]' => 'ios_x64',
            |        'KOTLIN_TARGET[sdk=iphoneos*]' => 'ios_arm64',
            |        'KOTLIN_TARGET[sdk=macosx*]' => 'macos_x64'
            |    }
            |
            |    preserve_path_patterns = ['*.gradle', 'gradle*', '*.properties', 'src/**/*.*']
            |    spec.preserve_paths = preserve_path_patterns + ['src/**/*']
            |
            |    spec.script_phases = [
            |        {
            |            :name => 'Build $specName',
            |            :execution_position => :before_compile,
            |            :shell_path => '/bin/sh',
            |            :script => <<-SCRIPT
            |                set -ev
            |                REPO_ROOT=`realpath "${'$'}PODS_TARGET_SRCROOT"`
            |                ${'$'}REPO_ROOT/gradlew -p "${'$'}REPO_ROOT" syncFramework -i      \
            |                    -P${KotlinCocoapodsPlugin.TARGET_PROPERTY}=${'$'}KOTLIN_TARGET \
            |                    -P${KotlinCocoapodsPlugin.CONFIGURATION_PROPERTY}=${'$'}CONFIGURATION
            |            SCRIPT
            |        }
            |    ]
            |end
        """.trimMargin())
    }
}

/**
 * Creates a dummy framework into the target directory.
 * This framework is used during Cocoapods install process
 * since the real framework isn't built yet at this stage.
 */
open class DummyFrameworkTask: DefaultTask() {
    @OutputDirectory
    val destinationDir = project.cocoapodsBuildDirs.framework

    @get:Input
    val frameworkName
        get() = project.name.replace('-', '_')

    private val frameworkDir: File
        get() = destinationDir.resolve("$frameworkName.framework")

    private fun copyResource(from: String, to: File) {
        to.parentFile.mkdirs()
        to.outputStream().use {
            javaClass.getResourceAsStream(from).copyTo(it)
        }
    }

    private fun copyFrameworkFile(relativeFrom: String, relativeTo: String = relativeFrom) =
        copyResource(
            "/cocoapods/dummy.framework/$relativeFrom",
            frameworkDir.resolve(relativeTo)
        )

    @TaskAction
    fun create() {
        // Reset the destination directory
        with(destinationDir) {
            deleteRecursively()
            mkdirs()
        }

        // Copy files for the dummy framework.
        copyFrameworkFile("Info.plist")
        copyFrameworkFile("dummy", frameworkName)
        copyFrameworkFile("Modules/module.modulemap")
        copyFrameworkFile("Headers/dummy.h", "Headers/$frameworkName.h")
    }
}