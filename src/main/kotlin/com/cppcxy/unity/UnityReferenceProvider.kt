package com.cppcxy.unity

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.LuaTypes
import com.tang.intellij.lua.reference.LuaRequireReference

class UnityReferenceProvider : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement().withElementType(LuaTypes.CALL_EXPR),
            CallExprReferenceProvider()
        )
    }

    internal inner class CallExprReferenceProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(
            psiElement: PsiElement,
            processingContext: ProcessingContext
        ): Array<PsiReference> {
//            val expr = psiElement as LuaCallExpr
//            val nameRef = expr.expr
//            if (nameRef is LuaNameExpr) {
//                if (LuaSettings.isRequireLikeFunctionName(nameRef.getText())) {
//                    return arrayOf(LuaRequireReference(expr))
//                }
//            }
            return PsiReference.EMPTY_ARRAY
        }
    }
}