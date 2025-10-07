package com.example.demoplugin.demoplugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.Topic
import com.sun.speech.freetts.Voice
import com.sun.speech.freetts.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.swing.JComponent
import javax.swing.JLabel

class ResultDialog:DialogWrapper(true) {

    lateinit var exception: String

    init {
        init()
        title = "Confirmation"
    }

    override fun createCenterPanel(): JComponent {
        return JLabel("It looks like you are facing some issue. Do you want to find solution on ChatGPT?")
    }

    override fun doOKAction() {
        super.doOKAction()

    }

    override fun doCancelAction() {
        super.doCancelAction()
        Messages.showInfoMessage("Ok no issues", "Confirmation")
        this.close(CANCEL_EXIT_CODE)
    }

}

class MyGradleListener : ExternalSystemTaskNotificationListener {
    lateinit var resultDialog: ResultDialog
    val stateFlow= MutableStateFlow<Int>(-1)
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
        coroutineScope.launch {
            stateFlow.collect {it->
                when(it) {
                    0->{
                        ApplicationManager.getApplication().invokeLater {
                            if (resultDialog.isShowing) {
                                val stringBuilder= StringBuilder()
                                e.stackTrace.forEach { stringBuilder.append(it.toString()).append("\n") }
                                val stringSelection = StringSelection(stringBuilder.toString())
                                CopyPasteManager.getInstance().setContents(stringSelection)
                                BrowserUtil.browse("https://chatgpt.com/")
                                Messages.showInfoMessage("Error has been copied to clipboard.", "Error Info")
                                resultDialog.close(OK_EXIT_CODE)
                            }
                        }
                        stateFlow.value=-1
                        this.cancel()
                    }
                    2-> {
                        ApplicationManager.getApplication().invokeLater {
                            if (resultDialog.isShowing) {
                                resultDialog.doCancelAction()
                            }
                        }
                        stateFlow.value=-1
                        this.cancel()
                    }
                }
            }
        }
        val shouldSpeak= MyPluginSettingState.instance.enableVoice
        if(shouldSpeak) {
            ApplicationManager.getApplication().invokeLater {
                resultDialog = ResultDialog().apply {
                    val stringBuilder= StringBuilder()
                    e.stackTrace.forEach { stringBuilder.append(it.toString()).append("\n") }
                    exception=stringBuilder.toString()
                }
                resultDialog.show()
            }
            System.setProperty(
                "freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory"
            )
            val voice: Voice? = VoiceManager.getInstance().getVoice("kevin16")
            voice?.allocate()
            val thread=object : Thread(){
                override fun run() {
                    speakText(voice,"It looks like you are facing some issue. Do you want to find a solution on Chat G P T?")
                    val result=takeUserInput()
                    stateFlow.value=result!!
                    when(result) {
                        0-> {
                            speakText(voice,"Error has been copied to clipboard.")
                            voice?.deallocate()
//                            val stringSelection = StringSelection(e.message)
//                            CopyPasteManager.getInstance().setContents(stringSelection)
//                            BrowserUtil.browse("https://chatgpt.com/")
                        }
                        2-> {
                            speakText(voice, "Ok no issues")
                            voice?.deallocate()
                        }
                    }
                }
            }
            thread.start()
        }
        else {
            ApplicationManager.getApplication().invokeLater {
                val result = Messages.showOkCancelDialog(
                    "It looks like you are facing some issue. Do you want to find solution on ChatGPT?",
                    "Confirmation",
                    "Yes",
                    "No",
                    Messages.getQuestionIcon()
                )


                if (result == Messages.OK) {
                    val stringSelection = StringSelection(e.stackTrace.toString())
                    CopyPasteManager.getInstance().setContents(stringSelection)
                    Messages.showInfoMessage("Error has been copied to clipboard.", "Error Info")
                    BrowserUtil.browse("https://chatgpt.com/")
                } else {
                    Messages.showInfoMessage("Ok no issues", "Confirmation")
                }
            }
        }
    }

    private  fun speakText(voice: Voice?,text:String) {

        voice?.speak(text)

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


fun takeUserInput(): Int {
    val resourcePath = "vosk-model-small-en-us"
    val tempDir = Files.createTempDirectory("vosk-model").toFile()

    // Copy model files from JAR resources to a real directory
    val resourceUrl = MyGradleListener::class.java.classLoader.getResource(resourcePath)
        ?: throw IOException("Model resource not found in JAR")

    // When running from JAR, we need to walk entries inside it
    if (resourceUrl.protocol == "jar") {
        val jarPath = resourceUrl.path.substringAfter("file:").substringBefore("!")
        val jarFile = java.util.jar.JarFile(URLDecoder.decode(jarPath, "UTF-8"))
        jarFile.use { jar ->
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith(resourcePath + "/") && !entry.isDirectory) {
                    val outFile = File(tempDir, entry.name.removePrefix(resourcePath + "/"))
                    outFile.parentFile.mkdirs()
                    jar.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }
    else {
        // If running from IDE (not JAR)
        val src = Paths.get(resourceUrl.toURI()).toFile()
        src.copyRecursively(tempDir, overwrite = true)
    }

    val model = Model(tempDir.absolutePath)
    val recognizer = Recognizer(model, 16000.0f)
    val format = AudioFormat(16000f, 16, 1, true, false)
    val microphone = AudioSystem.getTargetDataLine(format)

    return try {
        microphone.open(format)
        microphone.start()

        val buffer = ByteArray(4096)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= 5000) {
            val bytesRead = microphone.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    val text = recognizer.result.lowercase()
                    if ("yes" in text || "ok" in text) return 0
                } else {
                    val partial = recognizer.partialResult.lowercase()
                    if ("yes" in partial || "ok" in partial) return 0
                }
            }
        }
        2
    } finally {
        microphone.drain()
        microphone.stop()
        microphone.close()
    }
}





