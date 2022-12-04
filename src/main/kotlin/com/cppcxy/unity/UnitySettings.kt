package com.cppcxy.unity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic.create
import com.intellij.util.xmlb.XmlSerializerUtil

interface UnitySettingsListener {
    fun onUnitySettingsChanged()
}

enum class UnityLuaFramework(val inner: String) {
    XLua("XLua"),
    ToLua("ToLua");
    override fun toString(): String {
        return inner
    }
}

@State(name = "UnitySettings", storages = [Storage("emmyluaunty.xml")])
class UnitySettings : PersistentStateComponent<UnitySettings> {
    var unityNamespace = listOf("UnityEngine", "System")
    var framework = UnityLuaFramework.XLua
    companion object {
        val TOPIC = create("Unity settings changed.", UnitySettingsListener::class.java)

        @JvmStatic fun getInstance(): UnitySettings {
            return ApplicationManager.getApplication().getService(UnitySettings::class.java)
        }
    }



    fun getShowNamespace(): String {
        return unityNamespace.joinToString(";");
    }

    override fun getState(): UnitySettings? {
        return this
    }

    override fun loadState(settings: UnitySettings) {
        XmlSerializerUtil.copyBean(settings, this)
    }

    fun fireChanged() {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onUnitySettingsChanged()
    }
}