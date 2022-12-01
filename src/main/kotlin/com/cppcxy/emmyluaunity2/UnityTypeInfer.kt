package com.cppcxy.emmyluaunity2

import com.intellij.psi.PsiNamedElement
import com.intellij.util.Processor
import com.tang.intellij.lua.ext.ILuaTypeInfer
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassIndex
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.createSerializedClass

class UnityTypeInfer : ILuaTypeInfer {
    override fun inferType(target: LuaTypeGuessable, context: SearchContext): ITy {
        return when (target) {
            is LuaCallExpr -> {
                val name = (target.expr as? PsiNamedElement)?.name
                if (name == "GetComponent" && !context.isDumb) {
                    val arg = target.argList.firstOrNull()
                    if (arg is LuaLiteralExpr) {
                        val shortName = arg.stringValue
                        var ty: ITy = Ty.UNKNOWN
                        LuaClassIndex.processKeys(context.project, Processor {
                            if (it.endsWith(shortName)) {
                                ty = createSerializedClass(it)
                                return@Processor false
                            }
                            true
                        })
                        return ty
                    }
                }
                Ty.UNKNOWN
            }
            else -> Ty.UNKNOWN
        }
    }
}