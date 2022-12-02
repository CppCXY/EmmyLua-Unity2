package com.cppcxy.unity.extendApi
import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.psi.LuaClass
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass
import java.io.File
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import java.nio.charset.StandardCharsets

class ExtendShortNameManager : LuaShortNamesManager(), ProjectManagerListener {

    init {
        loadUnityProject()
    }

    val project: Project
        get() = ProjectManager.getInstance().openProjects.first()

    private fun loadUnityProject() {
        val workspaceDir = File(project.basePath)
        if (workspaceDir.isDirectory) {
            val slnFiles = workspaceDir.listFiles { it ->
                it.extension == "sln"
            }
            if(slnFiles.isNotEmpty()){
                val slnFile = slnFiles.first()
                val exePath = "C:\\Users\\zc\\Desktop\\github\\EmmyLua-Unity-LS\\unity\\bin\\Release\\net6.0\\unity.exe"
                val commandLine = GeneralCommandLine()
                    .withExePath(exePath)
                    .withParameters(
                        slnFile.path,
                        "UnityEngine"
                    )
                val handler = OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8))
                handler.addProcessListener(object : CapturingProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        val exitCode: Int = event.exitCode
                        if (exitCode == 0) {
                            val out = output.stdout
                            val params = Gson().fromJson(out, LuaReportApiParams::class.java)
                            ExtendApiService.loadApi(project, params)
                        }
                    }
                })
                handler.startNotify()
            }
        }
    }

    private fun findClass(name: String): NsMember? {
        return ExtendApiService.getExtendClasses()[name]
    }

    override fun findClass(name: String, context: SearchContext): LuaClass? {
        return findClass(name)
    }

    override fun findMember(type: ITyClass, fieldName: String, context: SearchContext): LuaClassMember? {
        val clazz = findClass(type.className) ?: return null
        return clazz.findMember(fieldName)
    }

    override fun processAllClassNames(project: Project, processor: Processor<String>): Boolean {
        val classes = ExtendApiService.getExtendClasses()
        for ((_, clazz) in classes) {
            if (!processor.process(clazz.fullName))
                return false
        }
        return true
    }

    override fun processClassesWithName(name: String, context: SearchContext, processor: Processor<LuaClass>): Boolean {
        return findClass(name, context)?.let { processor.process(it) } ?: true
    }

    override fun getClassMembers(clazzName: String, context: SearchContext): Collection<LuaClassMember> {
        val clazz = ExtendApiService.getNsMember(clazzName)
        if (clazz != null) {
            return clazz.members
        }
        return emptyList()
    }

    private fun processAllMembers(
        type: String,
        fieldName: String,
        context: SearchContext,
        processor: Processor<LuaClassMember>,
        deep: Boolean = true
    ): Boolean {
        val clazz = ExtendApiService.getNsMember(type) ?: return true
        val continueProcess = ContainerUtil.process(clazz.members.filter { it.name == fieldName }, processor)
        if (!continueProcess)
            return false

        if (clazz is ExtendClass) {
            val baseType = clazz.baseClassName
            if (deep && baseType != null) {
                return processAllMembers(baseType, fieldName, context, processor, deep)
            }
        }

        return true
    }

    override fun processAllMembers(
        type: ITyClass,
        fieldName: String,
        context: SearchContext,
        processor: Processor<LuaClassMember>
    ): Boolean {
        return processAllMembers(type.className, fieldName, context, processor)
    }
}