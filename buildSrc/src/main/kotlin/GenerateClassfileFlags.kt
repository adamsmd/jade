package org.ucombinator.jade.gradle

import org.jsoup.Jsoup
import java.net.URL

fun <A> List<A>.pairs(): List<Pair<A, A>> = (0..this.size step 2).map { Pair(this[it], this[it + 1]) }

// Code for generating `Flags.txt` and `Flags.kt`
object GenerateClassfileFlags {
  fun javaSpec(spec: String, version: Int, chapter: Int): String {
    if (version < 9) {
      throw Exception("The specified version must be at least 9, but version is $version.")
    }
    val url = "https://docs.oracle.com/javase/specs/$spec/se$version/html/$spec-$chapter.html"
    return URL(url).readText()
  }

  fun table(html: String): String {
    val builder = StringBuilder()
    val document = Jsoup.parse(html)

    builder.append(
      """
        |# Do not edit this file by hand.  It is generated by `sbt flagsTable`.
        |
        |# Kind      Name             Value  Keyword      Description
        |
      """.trimMargin()
    )

    val tables = listOf<Pair<String, String>>(
      "Class" to "Class access and property modifiers",
      "Field" to "Field access and property flags",
      "Method" to "Method access and property flags",
      "NestedClass" to "Nested class access and property flags",
    )
    for ((kind, tableSummary) in tables) {
      val (table) = document.select("""table[summary="$tableSummary"]""")
      for (row in table.select("tbody > tr").toList()) {
        val (accName, value, description) = row.select("td").toList()
        val keywordOption =
          """(Declared|Marked|Marked or implicitly) <code class="literal">(.*)</code>"""
            .toRegex()
            .find(description.childNodes().joinToString())
        val keyword = if (keywordOption === null) "-" else keywordOption.groupValues[2] // TODO: 2 -> 1?
        builder.append("$kind%-11s ${accName.text()}%-16s ${value.text()} $keyword%-12s ${description.text()}\n")
      }
      builder.append("\n")
    }

    val lists = listOf<Pair<String, String>>(
      "Parameter" to "access_flags",
      "Module" to "module_flags",
      "Requires" to "requires_flags",
      "Exports" to "exports_flags",
      "Opens" to "opens_flags",
    )
    for ((kind, codeLiteral) in lists) {
      val (list) = document
        .select(
          "dd:has(div[class=variablelist] dl) > p:matchesOwn(The value of the) > code[class=literal]:matchesOwn(^$codeLiteral$$)"
        )
      val rows = list.parent()!!.nextElementSibling()!!.child(0).children().pairs()
      for ((row, description) in rows) {
        val regexMatch = """(0x[0-9]*) \(([A-Z_]*)\)""".toRegex().find(row.text())!!
        val value = regexMatch.groupValues[1]
        val accName = regexMatch.groupValues[2]
        val keyword = if (accName == "ACC_TRANSITIVE") "transitive" else "-"
        builder.append("$kind%-11s $accName%-16s $value $keyword%-12s ${description.text()}\n")
      }
      builder.append("\n")
    }

    return builder.toString().replace("\n\n$", "\n")
  }

  private data class FlagInfo(
    val kind: String,
    val accName: String,
    val value: Int,
    val keyword: String?,
    val description: String
  )

  fun code(table: String): String {
    val flagInfos = table
      .lines()
      .filter { !it.matches("\\s*#.*".toRegex()) }
      .filter { !it.matches("\\s*".toRegex()) }
      .map {
        val (kind, accName, value, keyword, description) = it.split(" +".toRegex(), 5)
        val k = if (keyword == "-") null else keyword
        val intValue = value.substring(2).toInt(16)
        FlagInfo(kind, accName, intValue, k, description)
      }

    val flagInfoMap = flagInfos.groupBy { it.accName }
    val flagExtensions = flagInfoMap.mapValues { it.value.map { it.kind } }
    val uniqueFlagInfos = flagInfoMap.toList().map { it.second.first() }.sortedBy { it.accName }.sortedBy { it.value }
    val flagInfoGroups = mutableMapOf<String, List<FlagInfo>>()
    for (m in flagInfos) {
      flagInfoGroups.put(m.kind, flagInfoGroups.getOrDefault(m.kind, listOf()).plus(m))
    }

    val builder = StringBuilder()

    builder.append(
      """// Do not edit this file by hand.  It is generated by `gradle`.
        |
        |package org.ucombinator.jade.classfile
        |
        |import com.github.javaparser.ast.Modifier
        |import com.github.javaparser.ast.NodeList
        |
        |sealed interface Flag {
        |  fun value(): Int
        |  fun valueAsString(): String = "0x${'$'}{"%04x".format(value())}"
        |  fun keyword(): Modifier.Keyword?
        |  fun modifier(): Modifier? = if (keyword() === null) { null } else { Modifier(keyword()) }
        |}
        |
        |
      """.trimMargin()
    )

    for (kind in flagInfos.map { it.kind }.distinct()) {
      builder.append("sealed interface ${kind}Flag : Flag\n")
    }

    builder.append(
      """
        |object Flags {
        |  fun toModifiers(flags: List<Flag>): NodeList<Modifier> =
        |    NodeList(flags.mapNotNull { it.modifier() })
        |
        |  private fun <T> fromInt(mapping: List<Pair<Int, T>>): (Int) -> List<T> = { int ->
        |    val maskedInt = int and 0xffff // Ignore ASM specific flags, which occur above bit 16
        |    val result = mapping.filter { it.first and maskedInt != 0 }
        |    val intResult = result.fold(0) { x, y -> x or y.first }
        |    assert(maskedInt == intResult, { "flag parsing error: want 0x${'$'}{"%x".format(int)}, got 0x${'$'}{"%x".format(intResult)}" })
        |    result.map { it.second }
        |  }
        |
        |
      """.trimMargin()
    )

    for (flagsInfo in uniqueFlagInfos) {
      val keyword = if (flagsInfo.keyword === null) null else "Modifier.Keyword.${flagsInfo.keyword.toUpperCase()}"
      val extensions = flagExtensions.getValue(flagsInfo.accName).joinToString(", ") { "${it}Flag" }

      builder.append(
        """  object ${flagsInfo.accName} : Flag, $extensions {
          |    override fun value() = 0x${"%04x".format(flagsInfo.value)}
          |    override fun keyword() = $keyword
          |  }
          |
        """.trimMargin()
      )
    }

    for ((kind, flagInfosForKind) in flagInfoGroups) {
      assert(flagInfosForKind.map { it.value } == flagInfosForKind.map { it.value }.distinct())
      builder.append("  private val ${kind}Mapping = listOf<Pair<Int, ${kind}Flag>>(\n")
      for (flagInfo in flagInfosForKind.sortedBy { it.value }) {
        builder.append(
          "    /*0x${"%04x".format(flagInfo.value)}*/ ${flagInfo.accName}.value() to ${flagInfo.accName}, // ${flagInfo.description}\n"
        )
      }
      builder.append("  )\n")
    }
    builder.append("\n")

    for ((kind, _) in flagInfoGroups) {
      val name = "${kind.substring(0, 1).toLowerCase()}${kind.substring(1)}Flags"
      builder.append("  val $name: (Int) -> List<${kind}Flag> = fromInt(${kind}Mapping)\n")
    }
    builder.append("}\n")

    return builder.toString()
  }
}
