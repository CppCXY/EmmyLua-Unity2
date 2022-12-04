package com.cppcxy.unity

import com.cppcxy.unity.extendApi.ExtendApiService
import com.cppcxy.unity.extendApi.LuaReportApiParams
import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfoRt
import java.io.File
import java.nio.charset.StandardCharsets

object UnityLsAdapter {
    private val pluginSource: String?
        get() = PluginManagerCore.getPlugin(PluginId.getId("com.cppcxy.emmylua.unity"))?.path?.path


    private val unityLs: String = "$pluginSource/unity/bin/$unityLsInZip"

    private val unityLsInZip: String
        get() {
            return if (SystemInfoRt.isWindows) {
                "win32-x64/unity.exe"
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

    private fun resolveUnityLs(code: (String) -> Unit) {
        val path = unityLs

        val file = File(path)
        if (file.exists()) {
            code(path)
        }
//            } else {
//                // deprecated
//                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "download unity ls") {
//                    override fun run(indicator: ProgressIndicator) {
//                        indicator.fraction = 0.0
//                        indicator.text = "start download unity ls"
//                        val url = URL(unityLsUrl)
//                        url.openStream().use {
//                            indicator.fraction = 0.1
//                            indicator.text = "download unity ls ..."
//                            Files.copy(it, Paths.get("$pluginSource/temp.zip"))
//                        }
//                        indicator.fraction = 0.5 // halfway done
//                        indicator.text = "unzip ..."
//                        val zipFile = ZipFile("$pluginSource/temp.zip")
//                        val zipEntry = zipFile.getEntry(unityLsInZip)
//                        val inputStream = zipFile.getInputStream(zipEntry)
//                        var file = File(path)
//                        file.writeBytes(inputStream.readAllBytes())
//                        zipFile.close()
//
//                        code(path)
//                    }
//                })
//
//            }
        // }
    }

    fun loadUnityProject() {
        val workspaceDir = File(project.basePath)
        if (workspaceDir.isDirectory) {
            val slnFiles = workspaceDir.listFiles { it ->
                it.extension == "sln"
            }
            if (slnFiles.isNotEmpty()) {
                val slnFile = slnFiles.first()
                resolveUnityLs {

                    val commandLine = GeneralCommandLine()
                        .withExePath(it)
                        .withParameters(
                            slnFile.path,
                            UnitySettings.getInstance().unityNamespace.joinToString(";")
                        )

                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "load unity api ...") {
                        override fun run(indicator: ProgressIndicator) {
                            indicator.fraction = 0.5 // halfway done
                            indicator.text = "unity api loading..."

                            val handler = OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8))
                            handler.addProcessListener(object : CapturingProcessAdapter() {
                                override fun processTerminated(event: ProcessEvent) {
                                    val exitCode: Int = event.exitCode
                                    if (exitCode == 0) {
                                        val out = output.stdout
                                        val params = Gson().fromJson(out, LuaReportApiParams::class.java)
                                        ExtendApiService.loadApi(project, params)
                                        indicator.fraction = 1.0 // halfway done
                                        indicator.text = "unity api loaded"
                                    } else {
                                        indicator.text = output.stderr
                                        indicator.stop()
                                    }
                                }
                            })
                            handler.startNotify()
                        }
                    })
                }
            }
        }
    }

}