package com.example.demoplugin.demoplugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.Topic
import java.awt.datatransfer.StringSelection


class MyGradleListener : ExternalSystemTaskNotificationListener {
    override fun onStart(id: ExternalSystemTaskId) {

    }

    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {

    }

    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {

    }

    override fun onEnd(id: ExternalSystemTaskId) {

    }

    override fun onSuccess(id: ExternalSystemTaskId) {


    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
//        println("Gradle sync failed: ${e.message}")

        ApplicationManager.getApplication().invokeLater {
            val result = Messages.showOkCancelDialog(
                "It looks like you are facing some issue. Do you want to find solution on ChatGPT?", // message
                "Confirmation",            // title
                "Yes",                      // ok button text
                "No",                  // cancel button text
                Messages.getQuestionIcon() // icon
            )

            if (result == Messages.OK) {
                // OK was pressed, perform your action here
                val stringSelection = StringSelection(e.message)
                CopyPasteManager.getInstance().setContents(stringSelection)
                Messages.showInfoMessage("Error has been copied to clipboard.","Error Info")
                BrowserUtil.browse("https://chatgpt.com/")
            } else {
                // Cancel was pressed, perform your action here
                Messages.showInfoMessage("Ok no issues", "Confirmation")
            }
//            Messages.showInfoMessage(e.message, "Joke")
        }

    }

    override fun beforeCancel(id: ExternalSystemTaskId) {

    }

    override fun onCancel(id: ExternalSystemTaskId) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showInfoMessage("Cancelled", "Joke")
        }
    }
}

val GRADLE_TASK_NOTIFICATION_TOPIC = Topic.create(
    "Gradle Task Notification",
    ExternalSystemTaskNotificationListener::class.java
)

fun registerGradleListener(project: Project, listener: ExternalSystemTaskNotificationListener) {
    val connection = project.messageBus.connect()
    connection.subscribe(
        GRADLE_TASK_NOTIFICATION_TOPIC,
        listener
    )
}



