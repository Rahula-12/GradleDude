package com.example.demoplugin.demoplugin


import com.intellij.openapi.options.Configurable
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout

class MyPluginConfigurable : Configurable {

    private val settings = MyPluginSettingState.instance
    private var enableVoiceCheckbox: JCheckBox? = null

    override fun getDisplayName(): String = "Voice Control (Gradle Assistant)"

    override fun createComponent(): JComponent {
        val panel = JPanel(BorderLayout())
        enableVoiceCheckbox = JCheckBox("Enable voice control", settings.enableVoice)
        panel.add(enableVoiceCheckbox, BorderLayout.NORTH)
        return panel
    }

    override fun isModified(): Boolean {
        return enableVoiceCheckbox?.isSelected != settings.enableVoice
    }

    override fun apply() {
        settings.enableVoice = enableVoiceCheckbox?.isSelected == true
    }

    override fun reset() {
        enableVoiceCheckbox?.isSelected = settings.enableVoice
    }

    override fun disposeUIResources() {
        enableVoiceCheckbox = null
    }
}
