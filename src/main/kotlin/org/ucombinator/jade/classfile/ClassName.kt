package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.ClassOrInterfaceType

object ClassName {
  fun identifier(identifier: String): String = identifier.also {
    if (it == "") throw IllegalArgumentException("Empty identifier")
    if (it.any { it in ".;[/<>:" }) throw IllegalArgumentException("""Invalid identifier "${identifier}"""")
  }

  fun identifiers(string: String): List<String> = string.split('/').map(::identifier)

  fun className(string: String): Name = identifiers(string).fold(null, ::Name)!!

  fun classNameExpr(string: String): Expression =
    identifiers(string).map(::SimpleName).fold(null as Expression?) { qualifier, simpleName ->
      when (qualifier) {
        null -> NameExpr(simpleName)
        else -> FieldAccessExpr(qualifier, /*TODO*/ NodeList(), simpleName)
      }
    }!!

  fun classNameType(string: String): ClassOrInterfaceType = classNameType(className(string))

  fun classNameType(name: Name): ClassOrInterfaceType = classNameTypeOrNull(name)!!

  private fun classNameTypeOrNull(name: Name?): ClassOrInterfaceType? =
    if (name === null) null else ClassOrInterfaceType(classNameTypeOrNull(name.qualifier.orElse(null)), name.identifier)
}
