package org.ucombinator.jade.analysis

import org.ucombinator.jade.asm.Insn

// import org.ucombinator.jade.util.MyersList
// import org.ucombinator.jade.jgrapht.Dominator
// import org.jgrapht.traverse.DepthFirstIterator

/*
Non-Linear Stmt Types
  -Break
  -Continue
  -Return/Throw
  +Try-Catch-Finally
  +Synchronized
  +Do/While/For/For-each
  +If/Switch
Non-linear expressions
  Boolean(&&/||/!/==/!=/</>/<=/>=)
  Trinary Operator/Switch Expression
 */

typealias Nesting = List<Structure.Block>

// TODO: rename Structure to CodeStructure or CodeNesting or BlockNesting

/** Represents the domination structure in the CFG.
 *
 * @property nesting A map that associates each instruction with its nesting level within the CFG.
 * @property backEdges A set of edges in the CFG that keeps track of back edges.
 */
data class Structure(val nesting: Map<Insn, Nesting>, val backEdges: Set<ControlFlowGraph.Edge>) {
  // TODO: Instead of MyersList use the following or one of the others in that package:
  //  * https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/alg/lca/EulerTourRMQLCAFinder.html
  //      Preprocessing Time complexity: O(|V|log(|V|))
  //      Preprocessing Space complexity: O(|V|log(|V|))
  //      Query Time complexity: O(1)
  //      Query Space complexity: O(1)
  // TODO: name to NestingPath or StructurePath or BlockPath

  /** TODO:doc.
   *
   * @property kind TODO:doc
   * @property headInsn TODO:doc
   */
  data class Block(val kind: Kind, val headInsn: Insn)
  // case class Block(kind: Kind, headInsn: Insn, var parent: Block = null)

  /** TODO:doc. */
  sealed interface Kind {
    /** TODO:doc. */
    object Loop : Kind

    // TODO:
    // case class Exception(insns: List[Insn], handlers: List[(Insn, Type)]) extends Kind
    // handlers: dominated by head insn
    // body: dominated by head but not handlers
    // finally: ignore until refactoring pass
    // try ResourceSpecification Block [Catches] [Finally]

    /** TODO:doc. */
    object Exception : Kind

    // TODO:
    // Syncronized involves a try-finally pattern
    // case class Synchronized(value) extends Kind

    /** TODO:doc. */
    object Synchronized : Kind
  }

  companion object {
    /** TODO:doc.
     *
     * @param cfg TODO:doc
     * @return TODO:doc
     */
    fun make(cfg: ControlFlowGraph): Structure {
      // // This dummy works only on code with no loops, try/catches, or synchronized blocks
      // val backEdges = Set[ControlFlowGraph.Edge]()

      // // TODO: note that head block is present so we can always safely call
      // // .head, but its headInsn is null so it doesn't match the first
      // // instruction of the method

      // val nestingRoot: Nesting = MyersList.Cons(Block(kind = null, headInsn = null), MyersList.Nil)
      // val nestingMap = cfg.graph.vertexSet().asScala.map(_ -> nestingRoot).toMap
      // //val nestingMap: Map[Insn, Nesting] = Map.empty[Insn, Nesting]
      // //cfg.graph.vertexSet().asScala.map(_ -> nestingRoot).toMap

      // val dominatorTree = Dominator.dominatorTree(cfg.graph, cfg.entry)
      // val heads = Map[Insn, List[Kind]]()
      // // Inner heads are dominated by outer heads.

      // // val highestBlock = Map[Insn, Block]()
      // // val lowestBlock = Map[Insn, Block]()
      // // for (insn <- DepthFirstIterator(dominatorTree, cfg.entry).asScala) {
      // //   val backEdgeSet = cfg.graph.incomingEdgesOf(insn).filter { dominatorTree.dominatesSource(insn, it)) }
      // //   if (!backEdgeSet.empty) {
      // //     // backEdges ++= backEdgeSet
      // //     def addBlock(insn: Insn): Set[Insn] = {
      // //       highestBlock[insn] match {
      // //         case None =>
      // //           // We are first ones here, so setup both highest and lowest
      // //           highestBlock(insn) = block
      // //           lowestBlock(insn) = block
      // //           cfg.graph.incomingEdgesOf(insn).map(e => cfg.graph.getEdgeSource(e))
      // //           // TODO: idea: if we filter by domination of the head, we might allow multiple entry points
      // //         case Some(oldBlock) =>
      // //           if (oldBlock == block) { /* We've already processed this node */ }
      // //           else {
      // //             oldBlock.parent = structure
      // //             highestBlock(insn) = block
      // //             Set(oldBlock.head)
      // //           }
      // //       }
      // //     }
      // //     def addBlockBackwards(insn: Insn): Unit = {
      // //       val nextInsns = addBlock(insn)
      // //       nextInsns.foreach(addBlockBackwards)
      // //     }
      // //     val block = Block(Loop(), insn)
      // //     addBlock(insn)
      // //     backEdgeSet.foreach(addBlockBackwards)
      // //   }
      // //   return only lowestBlock
      // // }

      // return Structure(nestingMap, backEdges)
      // /*
      //     Loop heads dominate a predicesor
      //     Loop tree based on Dominator tree?
      //     Whole loop = all vertecies backwards from predicestor until loop head
      //  */
      TODO()
    }
  }
}

/** TODO:doc. */
object Exceptions {
  // def apply(cfg: ControlFlowGraph): Unit = {
  //   // TODO: check that handlers properly nest
  //   val union = new AsGraphUnion(cfg.graph, ???) // Edge from entry of try to each handler?
  //   val doms = Dominator.dominatorTree(union, cfg.entry)
  //   for (handler <- cfg.method.tryCatchBlocks.asScala) yield {
  //     val insns = new DepthFirstIterator[Insn, Dominator.Edge[Insn]](
  //       doms, Insn(cfg.method, handler.handler)).asScala.toList.sortBy(_.index)
  //     val indexes = insns.map(_.index)
  //     assert(indexes == (indexes.min to indexes.max).toList)
  //     handler -> insns
  //   }
  // }
}
