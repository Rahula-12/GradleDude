package com.example.demoplugin.demoplugin

import org.vosk.Model
import org.vosk.Recognizer
import javax.sound.sampled.*

fun startVoiceRecognition() {
    // Path to your downloaded model
    val modelPath = "src/main/resources/vosk-model-small-en-us-0.15"

    val model = Model(modelPath)
    val recognizer = Recognizer(model, 16000.0f)

    // Configure microphone
    val format = AudioFormat(16000f, 16, 1, true, false)
    val microphone = AudioSystem.getTargetDataLine(format)

    microphone.open(format)
    microphone.start()

    val buffer = ByteArray(4096)

    println("🎤 Speak into the microphone...")


    val bytesRead = microphone.read(buffer, 0, buffer.size)
    if (bytesRead > 0) {
        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
            println("✅ Final: ${recognizer.result}")
        } else {
            println("➡️ Partial: ${recognizer.partialResult}")
        }
    }

}
