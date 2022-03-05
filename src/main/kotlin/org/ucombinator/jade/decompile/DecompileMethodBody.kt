package org.ucombinator.jade.decompile

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.InitializerDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.ThrowStmt
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.Structure
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.javaparser.JavaParser
import org.ucombinator.jade.jgrapht.Dominator
import org.ucombinator.jade.jgrapht.GraphViz
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object DecompileMethodBody {
  private val log = Log.log {}
  private fun stubBody(message: String, comment: BlockComment?): BlockStmt {
    val statements = NodeList<Statement>(
      JavaParser.setComment(
        ThrowStmt(
          ObjectCreationExpr(
            null,
            ClassName.classNameType("java.lang.UnsupportedOperationException"),
            NodeList(StringLiteralExpr(message))
          )
        ),
        comment
      )
    )
    if (true) { // TODO: option for generating compilable versus uncompilable stub bodies
      statements.add(
        JavaParser.setComment(
          EmptyStmt(),
          BlockComment(" The following is unreachable code so that this code generates a compilation error ")
        )
      )
    }
    return BlockStmt(statements)
  }

  fun decompileBodyStub(node: MethodNode): BlockStmt {
    val instructions = run {
      if (node.instructions.size() == 0) {
        "   <no instructions>" // TODO: check
      } else {
        val textifier = Textifier()
        node.accept(TraceMethodVisitor(null, textifier))
        val stringWriter = StringWriter()
        textifier.print(PrintWriter(stringWriter))
        stringWriter.toString()
      }
    }
    return stubBody(
      "Jade decompiler generated only a stub implementation",
      BlockComment(
        """ This is a stub implementation generated by the Jade decompiler.
          |         *
          |         * Max Stack: ${node.maxStack}
          |         * Max Locals: ${node.maxLocals}
          |         * Instructions:
          |${instructions.lines().map { "         *$it" }.joinToString("\n")}
          |
        """.trimMargin()
      )
    )
  }

  fun setDeclarationBody(declaration: BodyDeclaration<out BodyDeclaration<*>>, body: BlockStmt) {
    when (declaration) {
      is InitializerDeclaration -> declaration.setBody(body)
      is ConstructorDeclaration -> declaration.setBody(body)
      is MethodDeclaration -> declaration.setBody(body)
      else -> Errors.unmatchedType(declaration)
    }
  }

  fun decompileBody(
    classNode: ClassNode,
    method: MethodNode,
    declaration: BodyDeclaration<out BodyDeclaration<*>>
  ) {
    if (method.instructions.size() == 0) {
      // The method has no body as even methods with empty bodies have a `return` instruction
      this.log.debug("**** Method is has no body ****")

      fun warningBody(warning: String): BlockStmt =
        if (false) { // TODO: option for fatal error vs uncompilable body vs compilable body
          this.log.error(warning)
          throw Exception(warning) // TODO
        } else {
          this.log.warn(warning)
          stubBody(warning, null)
        }

      when (declaration) { // TODO: use setDeclarationBody
        is InitializerDeclaration ->
          declaration.setBody(warningBody("No implementation for the static initializer for class ${classNode.name}"))
        is ConstructorDeclaration ->
          declaration.setBody(
            warningBody(
              "No implementation for constructor ${classNode.name}" +
                "(signature = ${method.signature}, descriptor = ${method.desc})"
            )
          )
        is MethodDeclaration -> {
          val modifiers = declaration.modifiers
          // TODO: if !(*.sym)
          if (modifiers.contains(Modifier.abstractModifier()) || modifiers.contains(Modifier.nativeModifier())) {
            declaration.setBody(null)
          } else {
            declaration.setBody(
              warningBody(
                "No implementation for non-abstract, non-native method: ${classNode.name}.${method.name}" +
                  "(signature = ${method.signature}, descriptor = ${method.desc})"
              )
            )
          }
        }
        else -> Errors.unmatchedType(declaration)
      }
    } else {
      this.log.debug("**** Method has a body with ${method.instructions.size()} instructions ****")
      this.log.debug("**** ControlFlowGraph ****")

      // loop via dominators (exit vs return is unclear)
      // if
      // switch via dominators
      // catch via dominators
      // synchronized: via CFG (problem: try{sync{try}}?)

      val cfg = ControlFlowGraph.make(classNode.name, method)

      this.log.debug("++++ cfg ++++\n${GraphViz.toString(cfg)}")
      for (v in cfg.graph.vertexSet()) {
        this.log.debug("v: ${cfg.graph.incomingEdgesOf(v).size}: $v")
      }

      this.log.debug("**** SSA ****")
      val ssa = StaticSingleAssignment.make(classNode.name, method, cfg)

      this.log.debug("++++ frames: ${ssa.frames.size} ++++")
      for (i in 0 until method.instructions.size()) {
        this.log.debug("frame($i): ${ssa.frames.get(i)}")
      }

      this.log.debug("++++ results and arguments ++++")
      for (i in 0 until method.instructions.size()) {
        val insn = method.instructions.get(i)
        this.log.debug("args($i): ${Insn.longString(method, insn)} --- ${ssa.insnVars.get(insn)}")
      }

      this.log.debug("++++ ssa map ++++")
      for ((key, value) in ssa.phiInputs) {
        this.log.debug("ssa: $key -> $value")
      }

      this.log.debug("**** Dominators ****")
      val doms = Dominator.dominatorTree(cfg.graphWithExceptions, cfg.entry)

      this.log.debug("++++ dominator tree ++++\n${GraphViz.toString(doms)}")

      this.log.debug(
        "++++ dominator nesting ++++\n${GraphViz.nestingTree(cfg.graphWithExceptions, doms, cfg.entry)}"
      )

      this.log.debug("**** Structure ****")
      val structure = Structure.make(cfg)

      // TODO: JEP 334: JVM Constants API: https://openjdk.java.net/jeps/334

      this.log.debug("**** Statement ****")
      val statement = DecompileStatement.make(cfg, ssa, structure)
      this.log.debug(statement.toString())
      setDeclarationBody(declaration, statement)

      // var statements = List[Statement]()
      // for (insn in method.instructions.toArray) {
      //   val (retVal, decompiled) = DecompileInsn.decompileInsn(insn, ssa)
      //   statements = statements :+ DecompileInsn.decompileInsn(retVal, decompiled)
      // }
      // this.log.debug("++++ statements ++++\n" + statements.mkString("\n"))
      // setDeclarationBody(declaration, new BlockStmt(new NodeList[Statement](statements.asJava)))
    }
  }
}
