package com.beok.processor

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance

fun visitTypeArguments(
    typeArguments: List<KSTypeArgument>,
    error: (String, KSNode) -> Unit
): String {
    var result = ""
    if (typeArguments.isEmpty()) return result
    result += "<"
    typeArguments.forEach { arg ->
        result += "${visitTypeArgument(arg, error)}, "
    }
    result += ">"
    return result
}

private fun visitTypeArgument(
    typeArgument: KSTypeArgument,
    error: (String, KSNode) -> Unit
): String {
    var result = when (val variance: Variance = typeArgument.variance) {
        Variance.STAR -> "*"
        Variance.INVARIANT -> ""
        Variance.COVARIANT,
        Variance.CONTRAVARIANT -> variance.label
    }
    if (result.endsWith("*").not()) {
        val resolvedType = typeArgument.type?.resolve()
        result += resolvedType?.declaration?.qualifiedName?.asString() ?: run {
            error("Invalid type argument", typeArgument)
        }

        val genericArgument = typeArgument.type?.element?.typeArguments ?: emptyList()
        result += visitTypeArguments(genericArgument, error)

        result += if (resolvedType?.nullability == Nullability.NULLABLE) "?" else ""
    }
    return result
}
