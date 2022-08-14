package com.beok.processor

import com.beok.annotations.AutoBuilder
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.validate

class AutoBuilderSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // AutoBuilder annotation 객체 find
        val symbols: Sequence<KSClassDeclaration> = resolver
            .getSymbolsWithAnnotation(AutoBuilder::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        // 객체 존재 여부 확인
        if (!symbols.iterator().hasNext()) return emptyList()

        symbols.forEach { symbol ->
            if (symbol.modifiers.none { it.name.equals(other = "data", ignoreCase = true) }) {
                logger.error(message = "You should write this function on a data class", symbol)
                return emptyList()
            }

            val flexible = flexible(symbol.annotations)
            if (flexible) {
                symbol.accept(MutableCreatorVisitor(codeGenerator, logger), Unit)
            }
            symbol.accept(AutoBuilderVisitor(codeGenerator, logger, flexible), Unit)
        }
        return symbols.filterNot(KSClassDeclaration::validate)
            .toList()
    }

    private fun flexible(annotations: Sequence<KSAnnotation>): Boolean {
        var annotation: KSAnnotation? = null
        annotations.forEach {
            if (it.shortName.asString() == AutoBuilder::class.java.simpleName) {
                annotation = it
                return@forEach
            }
        }
        if (annotation == null) {
            throw NoSuchElementException("Sequence contains no element matching the predicate.")
        }

        var argument: Boolean? = null
        annotation!!.arguments.forEach {
            if (it.name?.asString() == AutoBuilder.flexible) {
                argument = it.value as? Boolean
            }
        }
        if (argument == null) {
            throw NoSuchElementException("Sequence contains no element matching the predicate.")
        }
        return argument!!
    }
}
