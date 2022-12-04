package com.cppcxy.unity

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfoRt
import java.util.zip.ZipFile
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

object UnitySettings {
    private val pluginSource: String?
        get() = PluginManagerCore.getPlugin(PluginId.getId("com.cppcxy.emmylua.unity"))?.pluginPath?.toAbsolutePath().toString()


    private val unityLs: String
        get() {
            val base = "$pluginSource/unity"

            return if (SystemInfoRt.isWindows) {
                "$base/win/x64/unity.exe"
            } else if (SystemInfoRt.isMac) {
                if (System.getProperty("os.arch") == "arm64") {
                    "$base/mac/arm64/unity"
                } else {
                    "$base/mac/x64/unity"
                }
            } else {
                "$base/linux/x64/unity"
            }
        }

    private val unityLsInZip: String
        get() {
            return if (SystemInfoRt.isWindows) {
                "win32-64/unity.exe"
            } else if (SystemInfoRt.isMac) {
                if (System.getProperty("os.arch") == "arm64") {
                    "darwin-arm64/unity"
                } else {
                    "darwin-x64/unity"
                }
            } else {
                "linux-x64/unity"
            }
        }

    val unityNs = listOf("UnityEngine", "System")

    private val project: Project
        get() = ProjectManager.getInstance().openProjects.first()

    private const val version = "1.1.0"
    private val unityLsUrl: String
        get() {
            val base = "https://github.com/CppCXY/EmmyLua-Unity-LS/releases/download/$version/"
            return if (SystemInfoRt.isWindows) {
                "$base/win32-x64.zip"
            } else if (SystemInfoRt.isMac) {
                if (System.getProperty("os.arch") == "arm64") {
                    "$base/darwin-arm64.zip"
                } else {
                    "$base/darwin-x64.zip"
                }
            } else {
                "$base/linux-x64.zip"
            }
        }

    fun resolveUnityLs(code: (String) -> Unit) {
        val path = unityLs
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                code(path)
            } else {
                // deprecated
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "download unity ls") {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.fraction = 0.0
                        indicator.text = "start download unity ls"
                        val url = URL(unityLsUrl)
                        url.openStream().use {
                            indicator.fraction = 0.1
                            indicator.text = "download unity ls ..."
                            Files.copy(it, Paths.get("$pluginSource/temp.zip"))
                        }
                        indicator.fraction = 0.5 // halfway done
                        indicator.text = "unzip ..."
                        val zipFile = ZipFile("$pluginSource/temp.zip")
                        val zipEntry = zipFile.getEntry(unityLsInZip)
                        val inputStream = zipFile.getInputStream(zipEntry)
                        var file = File(path)
                        file.writeBytes(inputStream.readAllBytes())
                        zipFile.close()

                        code(path)
                    }
                })

            }
        }
    }

}