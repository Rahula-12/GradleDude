package com.example.demoplugin.demoplugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.vosk.Model
import org.vosk.Recognizer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

const val MODEL_PATH="/Users/rahularora/Downloads/demoPlugin_complete/src/main/resources/vosk-model-small-en-us"

class MyGradleListener : ExternalSystemTaskNotificationListener {

    val coroutineScope= CoroutineScope(Dispatchers.IO)

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
        val shouldSpeak= MyPluginSettingState.instance.enableVoice
        if(shouldSpeak) {
            System.setProperty(
                "freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory"
            )
            val voice: Voice? = VoiceManager.getInstance().getVoice("kevin16")
            voice?.allocate()
            coroutineScope.launch {
                speakText(voice,"It looks like you are facing some issue. Do you want to find a solution on Chat G P T?")
                val userInput=takeUserInput()
                when(userInput) {
                    1-> {

                        speakText(voice,"Error has been copied to clipboard.")

                        delay(1000)
                        voice?.deallocate()
                        val stringSelection = StringSelection(e.message)
                        CopyPasteManager.getInstance().setContents(stringSelection)
                            BrowserUtil.browse("https://chatgpt.com/")
                    }
                    0-> {
                            speakText(voice, "Ok no issues")

                        voice?.deallocate()
                    }
                }
            }
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
                val stringSelection = StringSelection(e.message)
                CopyPasteManager.getInstance().setContents(stringSelection)
                Messages.showInfoMessage("Error has been copied to clipboard.", "Error Info")
                BrowserUtil.browse("https://chatgpt.com/")
            } else {

                Messages.showInfoMessage("Ok no issues", "Confirmation")
            }
        }
        }
    }

    private suspend fun speakText(voice: Voice?,text:String) {
        withContext(Dispatchers.Default) {
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

fun unzip(zipFile: Path, targetDir: Path) {
    ZipInputStream(Files.newInputStream(zipFile)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val outPath = targetDir.resolve(entry.name)
            if (entry.isDirectory) {
                Files.createDirectories(outPath)
            } else {
                Files.createDirectories(outPath.parent)
                Files.copy(zip, outPath, StandardCopyOption.REPLACE_EXISTING)
            }
            entry = zip.nextEntry
        }
    }
}

fun getModelDir(): Path {
    val pluginDataDir = Path.of(PathManager.getPluginsPath(), "demoPlugin", "models")
    java.nio.file.Files.createDirectories(pluginDataDir)
    return pluginDataDir
}

suspend fun ensureModelExists(): Path {
    val modelDir = getModelDir().resolve("vosk-model-small-en-us")
    if (Files.exists(modelDir) && Files.isDirectory(modelDir)) {
        return modelDir
    }

    val modelZip = getModelDir().resolve("vosk-model-small-en-us.zip")
    val url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    println("⬇️ Downloading Vosk model... (this may take a while)")

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IllegalStateException("Failed to download model: ${response.code}")
        response.body?.byteStream()?.use { input ->
            Files.copy(input, modelZip, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    // unzip
    unzip(modelZip, getModelDir())

    return modelDir
}


 fun takeUserInput(): Int {
    val model = Model(MODEL_PATH)
    val recognizer = Recognizer(model, 16000.0f)
    val format = AudioFormat(16000f, 16, 1, true, false)
    val microphone = AudioSystem.getTargetDataLine(format)

    return try {
        microphone.open(format)
        microphone.start()

        val buffer = ByteArray(4096)
        val startTime = System.currentTimeMillis()
        println("🎤 Speak into the microphone...")

        while (System.currentTimeMillis() - startTime <= 5000) {
            val bytesRead = microphone.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    val text = recognizer.result.lowercase()
                    println("✅ Final: $text")
                    if ("yes" in text || "ok" in text) return 1
                } else {
                    val partial = recognizer.partialResult.lowercase()
                    println("🔹 Partial: $partial")
                    if ("yes" in partial || "ok" in partial) return 1
                }
            }
        }
        0
    } finally {
        microphone.drain()
        microphone.stop()
        microphone.close()
    }
}





