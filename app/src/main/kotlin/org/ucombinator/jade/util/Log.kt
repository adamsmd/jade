package org.ucombinator.jade.util

import mu.KotlinLogging
import mu.KLogger
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.CallerData
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.classic.Level
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter as OldHighlightingCompositeConverter
import ch.qos.logback.classic.Logger as LogbackLogger
import org.slf4j.Logger as Slf4jLogger
import org.slf4j.LoggerFactory

object Log {
  fun logger(func: () -> Unit) = KotlinLogging.logger(func)
  val prefix = "org.ucombinator.jade." // TODO: autodetect
  fun getLog(name: String): LogbackLogger {
    val modifiedName =
      if (name.isEmpty()) { Slf4jLogger.ROOT_LOGGER_NAME }
      else { name }
    return LoggerFactory.getLogger(modifiedName) as LogbackLogger
  }

  // val name = getClass.getName
  //   .replace('$', '.')
  //   .replace("..", ".")
  //   .replaceAll(".$", "")
  //   ScalaLogger(LoggerFactory.getLogger(name))
  // }
  // fun childLog(name: String): ScalaLogger = {
  //   ScalaLogger(LoggerFactory.getLogger(log.underlying.getName + "." + name))
  // }

//   def getLog(name: String): LogbackLogger = {
//     val modifiedName =
//       if (name.isEmpty) { Slf4jLogger.ROOT_LOGGER_NAME }
//       else { name }
//     LoggerFactory.getLogger(modifiedName).asInstanceOf[LogbackLogger]
//   }

//   def listLogs(): Unit = {
//     // See https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
//     // Note: toURI is required in order to handle special characters
//     val jar = new java.io.File(classOf[Main].getProtectionDomain.getCodeSource.getLocation.toURI).getPath
//     this.log.debug(f"jar: ${jar}")

//     for (entry <- new JarFile(jar).entries().asScala) {
//       if (entry.getName.endsWith(".class")) {
//         try {
//           Class.forName(entry.getName.replaceAll("\\.class$", "").replaceAll("/", "."))
//         } catch {
//           case _: Throwable => this.log.debug(f"skipping: ${entry.getName}") // TODO: show exception in message
//         }
//       }
//     }

//     for (
//       l <- LoggerFactory
//         .getLogger(Slf4jLogger.ROOT_LOGGER_NAME)
//         .asInstanceOf[LogbackLogger]
//         .getLoggerContext
//         .getLoggerList
//         .asScala
//     ) {
//       println(l.getName)
//     }
//   }

}

class RelativeLoggerConverter: ClassicConverter() {
  lateinit var prefix: String

  override fun start(): Unit {
    val x = getOptionList()
    assert(x.size == 1)
    prefix = x.get(0)
    super.start()
  }

  override fun convert(event: ILoggingEvent): String {
    val name = event.getLoggerName()
    if (name.startsWith(prefix)) { return name.removePrefix(prefix) }
    else { return "." + name }
  }
}

class DynamicCallerConverter: ClassicConverter() {
  companion object {
    var depthStart = 0
    var depthEnd = 0
  }

  override fun convert(event: ILoggingEvent): String {
    var buf = StringBuilder()
    var cda = event.getCallerData();
    if (cda != null && cda.size > depthStart) {
      val limit = if (depthEnd < cda.size) depthEnd else cda.size

      for (i in depthStart..limit - 1) {
          buf.append("Caller+");
          buf.append(i);
          buf.append("\t at ");
          buf.append(cda[i]);
          buf.append(CoreConstants.LINE_SEPARATOR);
      }
      return buf.toString();
    } else {
      return CallerData.CALLER_DATA_NA;
    }
  }
}


class HighlightingCompositeConverter: OldHighlightingCompositeConverter() {
  override protected fun getForegroundColorCode(event: ILoggingEvent): String =
    when (event.getLevel().toInt()) {
      Level.INFO_INT -> ANSIConstants.GREEN_FG
      Level.DEBUG_INT -> ANSIConstants.CYAN_FG
      Level.TRACE_INT -> ANSIConstants.MAGENTA_FG
      else -> super.getForegroundColorCode(event)
    }
}
