package org.ucombinator.jade.util

// initial implementation: dir or file only (no "inner" paths)

// url
// dir(traversedPath, remainingPath)

// zip(scan entire file)

// file(traversedPath, remainingPath, bytes)

// // TODO: caching
// // TODO: http, https, ftp, sftp, scp, git
// // TODO: gzip, tar, bzip2
// TODO:    jar:file:///path/to/MyJar.jar!/mypkg/MyClass.class per javap

// zip
//   .apk
//   .war
//   .ear
//   jar
//   jmod
// .tar
// .gz
// .bz2
// .xz
// .Z (compress)

// .iso
// .cab
// .dmg
// .rar

// https://stackoverflow.com/questions/315618/how-do-i-extract-a-tar-file-in-java
//   https://commons.apache.org/proper/commons-compress/examples.html
// https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/zip/package-summary.html
// https://commons.apache.org/proper/commons-vfs/
//   https://commons.apache.org/proper/commons-vfs/filesystems.html
// https://en.wikipedia.org/wiki/List_of_archive_formats

// https://commons.apache.org/proper/commons-vfs/filesystems.html
// https://commons.apache.org/proper/commons-vfs/index.html
// https://commons.apache.org/proper/commons-vfs/api.html
// https://commons.apache.org/proper/commons-vfs/apidocs/org/apache/commons/vfs2/FileSystemManager.html

// TODO:
// - https://github.com/skolson/KmpIO
// - https://developer.android.com/reference/kotlin/java/util/zip/ZipFile
// - https://github.com/alxiw/kotlin-archiver
// - https://stackoverflow.com/questions/69498191/android-11-kotlin-reading-a-zip-file
// - https://commons.apache.org/proper/commons-compress/apidocs/org/apache/commons/compress/compressors/CompressorStreamFactory.html
// - https://commons.apache.org/proper/commons-compress/apidocs/org/apache/commons/compress/archivers/ArchiveInputStream.html

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.ucombinator.jade.util.Lists.map

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/** TODO:doc. */
class ReadFiles {
  @Suppress("MAGIC_NUMBER", "VARIABLE_NAME_INCORRECT_FORMAT")
  companion object {
    private val CLASS_SIGNATURE = listOf<UByte>(0xcaU, 0xfeU, 0xbaU, 0xbeU).map { it.toByte() }
    private val JMOD_SIGNATURE =
      listOf<UByte>(0x4aU, 0x4dU, 0x01U, 0x00U, 0x50U, 0x4bU, 0x03U, 0x04U).map { it.toByte() }
    private const val JMOD_OFFSET = 4
  }

  // TODO: keep list of already traversed paths

  /** TODO:doc. */
  val result = mutableMapOf<List<File>, ByteArray>()

  /** TODO:doc. */
  val seen = mutableSetOf<File>()

  /** TODO:doc.
   *
   * @param file TODO:doc
   * @return TODO:doc
   */
  fun dir(file: File) {
    file
      .walk()
      .onEnter {
        if (seen.contains(it)) {
          false
        } else {
          seen.add(it)
          // match_file_branch
          true
        }
      }
      .filter { it.isFile() }
      // .filter { match_file_leaf }
      .forEach { name -> FileInputStream(name).use { read(listOf(name), it) } }
  }

  /** TODO:doc.
   *
   * @param inputStream TODO:doc
   * @param header TODO:doc
   * @return TODO:doc
   */
  fun headerMatches(inputStream: InputStream, header: List<Byte>): Boolean {
    require(inputStream.markSupported()) { "InputStream does not support mark: ${inputStream}" }
    inputStream.mark(header.size)
    // `ByteArray`s always compare as false (I don't know why), so we use List<Byte> instead
    val bytes = inputStream.readNBytes(header.size).toList()
    inputStream.reset()
    return bytes == header
  }

  // TODO: better handling of closing input streams

  /** TODO:doc.
   *
   * @param name TODO:doc
   * @param inputStream TODO:doc
   * @return TODO:doc
   */
  fun read(name: List<File>, inputStream: InputStream) {
    // We wrap in a BufferedInputStream to ensure we have support for `mark()` and `reset()`
    var input = BufferedInputStream(inputStream)
    while (true) {
      try {
        input = BufferedInputStream(CompressorStreamFactory().createCompressorInputStream(input))
      } catch (_: CompressorException) {
        break
      }
    }

    if (headerMatches(input, CLASS_SIGNATURE)) {
      // if (!match_entry(entryName.joinToString("\0"))) { continue; }
      result.put(name, input.readBytes())
      return
    }

    if (headerMatches(input, JMOD_SIGNATURE)) {
      input.readNBytes(JMOD_OFFSET)
    }
    // TODO: why does this break without the type annotation?
    val archive: ArchiveInputStream<out ArchiveEntry> = try {
      ArchiveStreamFactory().createArchiveInputStream(input)
    } catch (_: ArchiveException) {
      // TODO: log
      return
    }

    val entries = mutableMapOf<List<File>, ByteArrayInputStream>()
    while (true) {
      val entry: ArchiveEntry? = archive.nextEntry
      if (entry == null) break
      if (!archive.canReadEntryData(entry)) continue
      if (entry.isDirectory) continue
      val entryName = name + File(entry.name)
      // if (!match_entry(entryName.joinToString("\0"))) { continue; }
      entries[entryName] = ByteArrayInputStream(archive.readBytes())
    }

    // Note that we do this only after we have read the entire archive because
    // in some formats (e.g., `.zip`) the same filename could appear in multiple
    // entries.  We want to use the last entry with each filename.
    entries.toList().map(::read)
  }
}
