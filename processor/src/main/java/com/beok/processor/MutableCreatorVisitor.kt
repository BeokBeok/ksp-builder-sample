package com.beok.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability
import java.io.OutputStream

class MutableCreatorVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {

    private lateinit var file: OutputStream

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val fileName = "Mutable$className"

        file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false),
            packageName = packageName,
            fileName = fileName
        )
        file.write("package $packageName\n\n".toByteArray())

        file.write("internal class $fileName(\n".toByteArray())
        classDeclaration.getAllProperties()
            .forEach(::visitPropertyDeclaration)
        file.write(") {\n".toByteArray())
        file.write("\tfun to$className(): $className = $className(\n".toByteArray())
        classDeclaration.getAllProperties()
            .map { it.simpleName.asString() }
            .forEach {
                file.write("\t\t${it} = ${it},\n".toByteArray())
            }
        file.write("\t)\n".toByteArray())
        file.write("}".toByteArray())

        file.close()
    }

    private fun visitPropertyDeclaration(property: KSPropertyDeclaration) {
        val name: String = property.simpleName.asString()
        val typeResolve: KSType = property.type.resolve()
        val type: String = typeResolve.declaration.qualifiedName?.asString() ?: ""
        val nullable: String = if (typeResolve.nullability == Nullability.NULLABLE) "?" else ""
        val genericArguments: List<KSTypeArgument> =
            property.type.element?.typeArguments ?: emptyList()
        val generic: String = visitTypeArguments(genericArguments, logger::error)
        file.write("\tvar $name: $type$generic$nullable,\n".toByteArray())
    }
}
