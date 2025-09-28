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
import com.sun.speech.freetts.Voice
import com.sun.speech.freetts.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

const val MODEL_PATH="src/main/resources/vosk-model/vosk-model-small-en-us-0.15"

class MyGradleListener : ExternalSystemTaskNotificationListener {

    val coroutineScope= CoroutineScope(Dispatchers.Main)

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
        System.setProperty(
            "freetts.voices",
            "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory"
        )
        val voice: Voice? = VoiceManager.getInstance().getVoice("kevin16")
        val shouldSpeak= MyPluginSettingState.instance.enableVoice
        if(shouldSpeak)
        speakText(voice,"It looks like you are facing some issue. Do you want to find a solution on Chat G P T?")

        coroutineScope.launch {


                    val result = Messages.showOkCancelDialog(
                        "It looks like you are facing some issue. Do you want to find solution on ChatGPT?",
                        "Confirmation",
                        "Yes",
                        "No",
                        Messages.getQuestionIcon()
                    )


            if (result == Messages.OK) {
                if(shouldSpeak)  speakText(voice,"Error has been copied to clipboard.")
                val stringSelection = StringSelection(e.message)
                CopyPasteManager.getInstance().setContents(stringSelection)
                Messages.showInfoMessage("Error has been copied to clipboard.","Error Info")
                BrowserUtil.browse("https://chatgpt.com/")
            } else {
                if(shouldSpeak) speakText(voice,"Ok no issues")
                Messages.showInfoMessage("Ok no issues", "Confirmation")
            }
            voice?.deallocate()
        }
    }

    private fun speakText(voice: Voice?,text:String) {
        coroutineScope.launch(Dispatchers.Default) {
            voice?.allocate()
            voice?.speak(text)
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



