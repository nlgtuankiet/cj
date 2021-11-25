package com.rainyseason.wmmp

import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class WorkManagerMinPeriodicPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = project.extensions.findByType(BaseExtension::class.java)
            ?: throw GradleException("Not an Android project")

        android.registerTransform(WorkManagerMinPeriodicTransform(android, project.logger))
    }
}