package com.example.demoplugin.demoplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "MyPluginSettingsState",
    storages = [Storage("MyPluginSettings.xml")]
)
class MyPluginSettingState : PersistentStateComponent<MyPluginSettingState.State> {

    data class State (
        val enableVoice: Boolean = true
           // default ON
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var enableVoice: Boolean
        get() = myState.enableVoice
        set(value) {
            myState=myState.copy(value)
        }

    companion object {
        val instance: MyPluginSettingState
            get() = com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(MyPluginSettingState::class.java)
    }
}
