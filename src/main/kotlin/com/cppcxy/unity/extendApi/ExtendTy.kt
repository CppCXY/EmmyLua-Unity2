package com.cppcxy.unity.extendApi

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.light.LightElement
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.psi.LuaClass
import com.tang.intellij.lua.psi.LuaClassField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.Visibility
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class ExtendClassMember(
    val fieldName: String,
    val type: ITy,
    val parent: ExtendClass,
    private val comment: String,
    private val location: String,
    mg: PsiManager
) : LightElement(mg, LuaLanguage.INSTANCE), PsiNamedElement, LuaClassField, ExtendApiBase {
    override fun getComment(): String {
        return comment
    }

    override fun getLocation(): String {
        return location
    }

    override fun toString(): String {
        return fieldName
    }

    override fun guessType(context: SearchContext): ITy {
        return type
    }

    override fun setName(name: String): PsiElement {
        return this
    }

    override fun getName() = fieldName

    override fun guessParentType(context: SearchContext): ITy {
        return parent.type
    }

    override fun canNavigateToSource(): Boolean = true

    override fun navigate(requestFocus: Boolean) {
        val loc = location.substring(8).split('#') // file:///
        val path = loc[0]
        val offset = loc[1].toInt()
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        if (file != null) {
            val navigate = PsiNavigationSupport.getInstance().createNavigatable(project, file, offset)
            navigate.navigate(true)
        }
    }

    override val visibility: Visibility
        get() = Visibility.PUBLIC
    override val worth: Int
        get() = 0
    override val isDeprecated: Boolean
        get() = false
}

class TyExtendClass(val clazz: ExtendClass) : TyClass(
    clazz.fullName,
    clazz.name,
    clazz.baseClassName,
) {
    override fun findMemberType(name: String, searchContext: SearchContext): ITy? {
        return clazz.findMember(name)?.guessType(searchContext)
    }

    override fun findMember(name: String, searchContext: SearchContext): LuaClassMember? {
        return clazz.findMember(name)
    }

//    override fun getClassCallType(context: SearchContext): ITyFunction? {
//        val ty = clazz.findMember(".ctor")
//        if(ty is ExtendClassMember){
//            val funTy = ty.type
//            if(funTy is ITyFunction){
//                return funTy
//            }
//        }
//        return null
//    }
}

class ExtendClass(
    className: String,
    val fullName: String,
    val baseClassName: String?,
    parent: Namespace?,
    private val comment: String,
    private val location: String,
    private val attribute: String,
    mg: PsiManager
) : NsMember(className, parent, mg) {

    private val ty: ITyClass by lazy { TyExtendClass(this) }
    private val methods = mutableMapOf<String, MutableList<IFunSignature>>()

    override val type: ITyClass
        get() = ty
    override val worth: Int
        get() = 0

    override fun toString(): String {
        return fullName
    }

    fun addMember(name: String, type: ITy, comment: String, location: String) {
        val member = ExtendClassMember(name, type, this, comment, location, manager)
        members.add(member)
    }

    fun addMethod(name: String, signature: FunSignature, comment: String, location: String) {
        if (!methods.containsKey(name)) {
            val ty = TyExtendFunction(this, name)
            methods[name] = mutableListOf(signature)
            addMember(name, ty, comment, location)
        } else {
            methods[name]?.add(signature)
        }
    }

    fun getMethods(name: String): MutableList<IFunSignature>? {
        return methods[name]
    }

    override fun canNavigateToSource(): Boolean = true

    override fun navigate(requestFocus: Boolean) {
        val loc = location.substring(8).split('#') // file:///
        val path = loc[0]
        val offset = loc[1].toInt()
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        if (file != null) {
            val navigate = PsiNavigationSupport.getInstance().createNavigatable(project, file, offset)
            navigate.navigate(true)
        }
    }

    val isEnum: Boolean
        get() = attribute == "enum"

    val isInterface: Boolean
        get() = attribute == "interface"

    val isDelegate: Boolean
        get() = attribute == "delegate"

    override fun getComment(): String {
        return comment
    }

    override fun getLocation(): String {
        return location
    }

}

abstract class NsMember(
    val memberName: String,
    val parent: Namespace?,
    mg: PsiManager
) : LightElement(mg, LuaLanguage.INSTANCE), PsiNamedElement, LuaClass, LuaClassField, ExtendApiBase {

    val members = mutableListOf<LuaClassMember>()

    override fun setName(name: String): PsiElement {
        return this
    }

    override fun getName(): String {
        return memberName
    }

    override fun guessType(context: SearchContext?): ITy {
        return type
    }

    override fun guessParentType(context: SearchContext): ITy {
        return parent?.type ?: Ty.UNKNOWN
    }

    fun getMember(name: String): LuaClassMember? {
        return members.firstOrNull { it.name == name }
    }

    fun findMember(name: String): LuaClassMember? {
        return members.firstOrNull { it.name == name }
    }

    override val visibility: Visibility
        get() = Visibility.PUBLIC
    override val isDeprecated: Boolean
        get() = false
}

private class NamespaceType(val namespace: Namespace) : TyClass(namespace.fullName) {
    override fun findMemberType(name: String, searchContext: SearchContext): ITy? {
        return namespace.getMember(name)?.guessType(searchContext)
    }

    override fun findMember(name: String, searchContext: SearchContext): LuaClassMember? {
        return namespace.getMember(name)
    }

    override val displayName: String
        get() = namespace.toString()
}

class Namespace(
    val typeName: String,
    parent: Namespace?,
    mg: PsiManager,
    val isValidate: Boolean
) : NsMember(typeName, parent, mg), LuaClass, LuaClassField {

    private val myType by lazy { NamespaceType(this) }
    private val myMembers = mutableMapOf<String, Namespace>()
    private val myClasses = mutableListOf<ExtendClass>()

    val fullName: String
        get() {
            return if (parent == null || !parent.isValidate) typeName else "${parent.fullName}.$typeName"
        }

    fun addMember(ns: String): Namespace {
        val member = Namespace(ns, this, myManager, true)
        myMembers[ns] = member
        members.add(member)
        return member
    }

    fun addMember(clazz: ExtendClass) {
        myClasses.add(clazz)
        members.add(clazz)
    }

    fun getOrPut(ns: String): Namespace {
        val m = myMembers[ns]
        if (m != null) return m
        return addMember(ns)
    }

    override val type: ITyClass
        get() = myType
    override val worth: Int
        get() = 0

    override fun toString(): String {
        return "namespace $fullName"
    }

    override fun getComment(): String {
        return toString()
    }

    override fun getLocation(): String {
        return ""
    }
}

class TyExtendFunction(
    private val clazz: ExtendClass,
    val name: String
) : TyFunction() {
    override val mainSignature: IFunSignature
        get() {
            val methods = clazz.getMethods(name)
            if (methods != null && methods.size >= 1) {
                return methods.first()
            }
            return FunSignature(true, Ty.create("void"), null, emptyArray())
        }

    override val signatures: Array<IFunSignature>
        get() {
            val methods = clazz.getMethods(name)
            if (methods != null) {
                return methods.toTypedArray()
            }
            return emptyArray()
        }
}