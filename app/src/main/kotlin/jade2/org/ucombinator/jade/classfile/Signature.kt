package org.ucombinator.jade.classfile

// import scala.jdk.CollectionConverters._

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.`type`.*
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.SimpleName
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Fourple
// import sun.reflect.generics.parser.SignatureParser
// import sun.reflect.generics.tree._

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor

// import SignatureLexer
import SignatureParser
import SignatureParser.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.ListTokenSource
import java.io.ByteArrayInputStream

object Signature {
  fun typeSignature(string: String): Type =
    convert(parser(string).javaTypeSignature())

  fun classSignature(string: String): Triple<List<TypeParameter>, ClassOrInterfaceType, List<ClassOrInterfaceType>> =
    convert(parser(string).classSignature())

  fun methodSignature(string: String): Fourple<List<TypeParameter>, List<Type>, Type, List<ReferenceType>> =
    convert(parser(string).methodSignature())

  private fun parser(string: String): SignatureParser =
    SignatureParser(CommonTokenStream(ListTokenSource(string.map { CommonToken(it.code, it.toString()) })))
    // SignatureParser(CommonTokenStream(SignatureLexer(CharStreams.fromString(string))))

  ////////////////////////////////////////////////////////////////
  // TODO

  fun convert(tree: BaseTypeContext): Type =
    when (tree) {
      is ByteContext -> PrimitiveType.byteType()
      is CharContext -> PrimitiveType.charType()
      is DoubleContext -> PrimitiveType.doubleType()
      is FloatContext -> PrimitiveType.floatType()
      is IntContext -> PrimitiveType.intType()
      is LongContext -> PrimitiveType.longType()
      is ShortContext -> PrimitiveType.shortType()
      is BooleanContext -> PrimitiveType.booleanType()
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: VoidDescriptorContext): VoidType =
    VoidType()

  ////////////////////////////////////////////////////////////////
  // Java type signature

  fun convert(tree: JavaTypeSignatureContext): Type =
    when (tree) {
      is JavaTypeReferenceContext -> convert(tree.referenceTypeSignature())
      is JavaTypeBaseContext -> convert(tree.baseType())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  ////////////////////////////////////////////////////////////////
  // Reference type signature

  fun convert(tree: ReferenceTypeSignatureContext): ReferenceType =
    when (tree) {
      is ReferenceTypeClassContext -> convert(tree.classTypeSignature())
      is ReferenceTypeVariableContext -> convert(tree.typeVariableSignature())
      is ReferenceTypeArrayContext -> convert(tree.arrayTypeSignature())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: ClassTypeSignatureContext): ClassOrInterfaceType =
    convertSuffix(
      tree.classTypeSignatureSuffix(),
      convert(
        tree.simpleClassTypeSignature(),
        convert(
          tree.packageSpecifier())))

  fun convert(tree: PackageSpecifierContext?): ClassOrInterfaceType? =
    when (tree) {
      null -> null
      else -> convert(listOf(tree), null)
    }

  fun convert(tree: List<PackageSpecifierContext>, scope: ClassOrInterfaceType?): ClassOrInterfaceType? =
    when {
      tree.isEmpty() -> scope
      else ->
        convert(
          // TODO: 'plus' causes quadratic behavior
          tree.first().packageSpecifier().plus(tree.subList(1, tree.size)),
          ClassOrInterfaceType(scope, tree.first().identifier().text))
    }

  fun convert(tree: SimpleClassTypeSignatureContext, scope: ClassOrInterfaceType?): ClassOrInterfaceType =
    ClassOrInterfaceType(
      scope,
      SimpleName(tree.identifier().text),
      convert(tree.typeArguments()))

  fun convert(tree: TypeArgumentsContext?): NodeList<Type>? =
    when (tree) {
      null -> null
      else -> NodeList(tree.typeArgument().map(::convert))
    }

  fun convert(tree: TypeArgumentContext): Type =
    when (tree) {
      is TypeArgumentNonStarContext -> convert(tree.wildcardIndicator(), convert(tree.referenceTypeSignature()))
      is TypeArgumentStarContext -> WildcardType()
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: WildcardIndicatorContext?, ref: ReferenceType): Type =
    when (tree) {
      null -> ref
      is WildcardPlusContext -> WildcardType(ref)
      is WildcardMinusContext -> WildcardType(null, ref, NodeList())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  // NOTE: Renamed to deconflict with convert(tree: List<PackageSpecifierContext>, scope: ClassOrInterfaceType?): ClassOrInterfaceType?
  fun convertSuffix(tree: List<ClassTypeSignatureSuffixContext>, scope: ClassOrInterfaceType): ClassOrInterfaceType =
    when {
      tree.isEmpty() -> scope
      else ->
        convertSuffix(
          tree.subList(1, tree.size),
          convert(
            tree.first().simpleClassTypeSignature(),
            scope))
    }

  fun convert(tree: ClassTypeSignatureSuffixContext, scope: ClassOrInterfaceType): ClassOrInterfaceType =
    convert(tree.simpleClassTypeSignature(), scope)

  fun convert(tree: TypeVariableSignatureContext): TypeParameter = // TODO: check 'TypeParameter'
    TypeParameter(tree.identifier().text)

  fun convert(tree: ArrayTypeSignatureContext): ArrayType =
    ArrayType(convert(tree.javaTypeSignature()))

  ////////////////////////////////////////////////////////////////
  // Class signature

  fun convert(tree: ClassSignatureContext): Triple<List<TypeParameter>, ClassOrInterfaceType, List<ClassOrInterfaceType>> =
    Triple(
      convert(tree.typeParameters()),
      convert(tree.superclassSignature()),
      tree.superinterfaceSignature().map(::convert))

  fun convert(tree: TypeParametersContext?): List<TypeParameter> =
    when (tree) {
      null -> listOf()
      else -> tree.typeParameter().map(::convert)
    }

  fun convert(tree: TypeParameterContext): TypeParameter =
    TypeParameter(
      tree.identifier().text,
      NodeList(
        convert(tree.classBound())
          .plus(tree.interfaceBound().map(::convert))))

  fun convert(tree: ClassBoundContext): List<ClassOrInterfaceType> = // NOTE: this list is zero or one element
    when (val ref = tree.referenceTypeSignature()) {
      null -> listOf()
      else -> listOf(convert(ref) as ClassOrInterfaceType)
    }

  fun convert(tree: InterfaceBoundContext): ClassOrInterfaceType =
    convert(tree.referenceTypeSignature()) as ClassOrInterfaceType

  fun convert(tree: SuperclassSignatureContext): ClassOrInterfaceType =
    convert(tree.classTypeSignature())

  fun convert(tree: SuperinterfaceSignatureContext): ClassOrInterfaceType =
    convert(tree.classTypeSignature())

  ////////////////////////////////////////////////////////////////
  // Method signature

  fun convert(tree: MethodSignatureContext): Fourple<List<TypeParameter>, List<Type>, Type, List<ReferenceType>> =
    Fourple(
      convert(tree.typeParameters()),
      tree.javaTypeSignature().map(::convert),
      convert(tree.result()),
      tree.throwsSignature().map(::convert))

  fun convert(tree: ResultContext): Type =
    when (tree) {
      is ResultNonVoidContext -> convert(tree.javaTypeSignature())
      is ResultVoidContext -> convert(tree.voidDescriptor())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: ThrowsSignatureContext): ReferenceType =
    when (tree) {
      is ThrowsClassContext -> convert(tree.classTypeSignature())
      is ThrowsVariableContext -> convert(tree.typeVariableSignature())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  ////////////////////////////////////////////////////////////////
  // Field signature

  // NOTE: unused
  fun convert(tree: FieldSignatureContext): ReferenceType =
    convert(tree.referenceTypeSignature())
}










// class Foo(api: Int) : SignatureVisitor(api) {
//   var stack;
//   var expr;

//   // Visits a signature corresponding to an array type.
//   override fun visitArrayType(): SignatureVisitor {}

//   // Visits a signature corresponding to a primitive type.
//   override fun visitBaseType(descriptor: Char): Unit {}

//   // Visits the class bound of the last visited formal type parameter.
//   override fun visitClassBound(): SignatureVisitor {}

//   // Starts the visit of a signature corresponding to a class or interface type.
//   override fun visitClassType(name: String): Unit {}

//   // Ends the visit of a signature corresponding to a class or interface type.
//   override fun visitEnd(): Unit {}

//   // Visits the type of a method exception.
//   override fun visitExceptionType(): SignatureVisitor {}

//   // Visits a formal type parameter.
//   override fun visitFormalTypeParameter(name: String): Unit {}

//   // Visits an inner class.
//   override fun visitInnerClassType(name: String): Unit {}

//   // Visits the type of an interface implemented by the class.
//   override fun visitInterface(): SignatureVisitor {}

//   // Visits an interface bound of the last visited formal type parameter.
//   override fun visitInterfaceBound(): SignatureVisitor {}

//   // Visits the type of a method parameter.
//   override fun visitParameterType(): SignatureVisitor {}

//   // Visits the return type of the method.
//   override fun visitReturnType(): SignatureVisitor {}

//   // Visits the type of the super class.
//   override fun visitSuperclass(): SignatureVisitor {}

//   // Visits an unbounded type argument of the last visited class or inner class type.
//   override fun visitTypeArgument(): Unit {}

//   // Visits a type argument of the last visited class or inner class type.
//   override fun visitTypeArgument(wildcard: Char): SignatureVisitor {}

//   // Visits a signature corresponding to a type variable.
//   override fun visitTypeVariable(name: String): Unit {}

// }
