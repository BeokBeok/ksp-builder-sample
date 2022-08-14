package com.beok.processor

import com.beok.annotations.BuilderProperty
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability
import java.io.OutputStream

class AutoBuilderVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val flexible: Boolean
) : KSVisitorVoid() {

    private lateinit var file: OutputStream

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val objectName = if (flexible) {
            "mutable$className"
        } else {
            className.replaceFirstChar(Char::lowercase)
        }
        val fileName = "${className}Builder"
        val targetName = if (flexible) "Mutable$className" else className

        file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false),
            packageName = packageName,
            fileName = fileName
        )

        file.write("package $packageName\n\n".toByteArray())

        file.write("class $fileName(\n".toByteArray())
        classDeclaration.getAllProperties()
            .forEach(::toCreateConstructor)
        file.write(") {\n\n".toByteArray())

        file.write("\tprivate val $objectName: $targetName = $targetName(\n".toByteArray())
        classDeclaration.getAllProperties()
            .forEach(::toCreatePrivateVariable)
        file.write("\t)\n\n".toByteArray())

        classDeclaration.getAllProperties()
            .forEach {
                toCreateBuilderFunctions(
                    property = it,
                    fileName = fileName,
                    objectName = objectName
                )
            }

        file.write("\tfun build(): $className = $objectName".toByteArray())
        file.write(if (flexible) ".to$className()\n".toByteArray() else "\n".toByteArray())
        file.write("\n}".toByteArray())

        file.close()
    }

    /*
    fun age(age: kotlin.Int): PersonBuilder {
        mutablePerson.age = age
        return this
    }
    제네릭이 있는 경우 - kotlin.collections.List<kotlin.Boolean>
     */
    private fun toCreateBuilderFunctions(
        property: KSPropertyDeclaration,
        fileName: String,
        objectName: String
    ) {
        property.annotations.forEach { annotation ->
            if (annotation.shortName.asString() != BuilderProperty::class.java.simpleName) {
                return@forEach
            }
            val name: String = property.simpleName.asString()
            val type: String = property.type.resolve().declaration.qualifiedName?.asString() ?: ""
            val genericArguments: List<KSTypeArgument> =
                property.type.element?.typeArguments ?: emptyList()
            val generic = visitTypeArguments(genericArguments, logger::error)

            file.write("\tfun $name($name: $type$generic): $fileName {\n".toByteArray())
            file.write("\t\t$objectName.$name = $name\n".toByteArray())
            file.write("\t\treturn this\n".toByteArray())
            file.write("\t}\n\n".toByteArray())
        }
    }

    /*
    private val mutablePerson: MutablePerson = MutablePerson(
        name = name,
        age = null,
        email = null,
        contract = null,
    )
     */
    private fun toCreatePrivateVariable(property: KSPropertyDeclaration) {
        val name: String = property.simpleName.asString()
        var hasAnnotation = false
        property.annotations.forEach { annotation ->
            if (annotation.shortName.asString() == BuilderProperty::class.java.simpleName) {
                hasAnnotation = true
                return@forEach
            }
        }
        file.write(
            if (hasAnnotation) {
                "\t\t$name = null,\n".toByteArray()
            } else {
                "\t\t$name = $name,\n".toByteArray()
            }
        )
    }

    /*
    class PersonBuilder(name: kotlin.String)
     */
    private fun toCreateConstructor(property: KSPropertyDeclaration) {
        val typeResolve = property.type.resolve()
        var hasAnnotation = false
        property.annotations.forEach { annotation ->
            if (annotation.shortName.asString() == BuilderProperty::class.java.simpleName) {
                hasAnnotation = true
                return@forEach
            }
        }
        if (hasAnnotation) {
            if ((typeResolve.nullability == Nullability.NULLABLE)) return
            logger.error("BuilderProperties have to be nullable", property)
            return
        }
        val name: String = property.simpleName.asString()
        val type: String = typeResolve.declaration.qualifiedName?.asString() ?: ""
        val nullable = if (typeResolve.nullability == Nullability.NULLABLE) "?" else ""
        val genericArguments: List<KSTypeArgument> =
            property.type.element?.typeArguments ?: emptyList()
        val generic = visitTypeArguments(genericArguments, logger::error)
        file.write("\t$name: $type$generic$nullable,\n".toByteArray())
    }
}
