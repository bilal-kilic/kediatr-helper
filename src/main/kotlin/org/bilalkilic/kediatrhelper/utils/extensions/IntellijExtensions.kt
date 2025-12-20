package org.bilalkilic.kediatrhelper.utils.extensions

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

fun KtClassOrObject.getSerialSuperClassNames(): List<String> = analyze(this) {
    val symbol = this@getSerialSuperClassNames.symbol as? KaClassSymbol ?: return@analyze emptyList()
    symbol.superTypes.mapNotNull { superType ->
        val classSymbol = superType.symbol as? KaClassSymbol
        classSymbol?.classId?.asSingleFqName()?.asString()
    }
}

fun PsiClass.getKtClass() = (this as? KtLightClassForSourceDeclaration)?.kotlinOrigin as? KtClass
