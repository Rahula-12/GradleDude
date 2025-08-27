package com.example.demoplugin.demoplugin

import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.gradle.BuildListener
import org.gradle.BuildResult

class MyStartupActivity:StartupActivity {
    override fun runActivity(project: Project) {
        val gradleListener=MyGradleListener()
        ExternalSystemProgressNotificationManager.getInstance()
            .addNotificationListener(gradleListener, project)
        registerGradleListener(project,MyGradleListener())
    }
}