/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.reader

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Leitor de EPUB implementado do zero (sem biblioteca externa), para manter o
 * app leve. Um EPUB é um .zip contendo XHTML; aqui nós:
 *  1) extraímos o zip para o cache do app (uma vez só, ficando salvo entre leituras);
 *  2) lemos META-INF/container.xml para achar o arquivo .opf;
 *  3) lemos o .opf para montar a ordem dos capítulos (spine).
 * A exibição em si é feita por uma WebView carregando cada capítulo extraído.
 */
class EpubBookReader private constructor(
    val spineFiles: List<File>
) {
    val chapterCount: Int get() = spineFiles.size

    fun chapterFile(index: Int): File? = spineFiles.getOrNull(index)

    companion object {
        private const val MANIFEST_CACHE_NAME = ".spine_manifest.txt"

        fun open(context: Context, uri: Uri, bookId: Long): EpubBookReader? {
            val extractDir = File(context.cacheDir, "epub_$bookId")
            val manifestCache = File(extractDir, MANIFEST_CACHE_NAME)

            if (extractDir.exists() && manifestCache.exists()) {
                val cached = manifestCache.readLines()
                    .filter { it.isNotBlank() }
                    .map { File(extractDir, it) }
                if (cached.isNotEmpty() && cached.all { it.exists() }) {
                    return EpubBookReader(cached)
                }
            }

            // Extração do zero (primeira vez, ou cache inválido/corrompido).
            extractDir.deleteRecursively()
            if (!extractDir.mkdirs()) return null

            return try {
                extractZip(context, uri, extractDir)
                val spineFiles = parseSpine(extractDir) ?: return null
                if (spineFiles.isEmpty()) return null
                saveManifestCache(manifestCache, extractDir, spineFiles)
                EpubBookReader(spineFiles)
            } catch (e: Exception) {
                null
            }
        }

        private fun extractZip(context: Context, uri: Uri, targetDir: File) {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Não foi possível abrir o EPUB")
            val canonicalTargetPrefix = targetDir.canonicalPath + File.separator

            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
                    val safe = outFile.canonicalPath.startsWith(canonicalTargetPrefix)

                    if (safe) {
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        private fun parseSpine(root: File): List<File>? {
            val containerFile = File(root, "META-INF/container.xml")
            if (!containerFile.exists()) return null
            val opfPath = findOpfPath(containerFile) ?: return null
            val opfFile = File(root, opfPath)
            if (!opfFile.exists()) return null
            return parseOpfSpine(opfFile, opfFile.parentFile ?: root)
        }

        private fun findOpfPath(containerFile: File): String? {
            val parser = Xml.newPullParser()
            containerFile.inputStream().use { stream ->
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG &&
                        parser.name?.substringAfterLast(':') == "rootfile"
                    ) {
                        return parser.getAttributeValue(null, "full-path")
                    }
                    event = parser.next()
                }
            }
            return null
        }

        private fun parseOpfSpine(opfFile: File, opfDir: File): List<File> {
            val manifest = HashMap<String, String>() // id -> href
            val spineIds = mutableListOf<String>()

            val parser = Xml.newPullParser()
            opfFile.inputStream().use { stream ->
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        when (parser.name?.substringAfterLast(':')) {
                            "item" -> {
                                val id = parser.getAttributeValue(null, "id")
                                val href = parser.getAttributeValue(null, "href")
                                if (id != null && href != null) manifest[id] = href
                            }
                            "itemref" -> {
                                parser.getAttributeValue(null, "idref")?.let { spineIds.add(it) }
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            return spineIds.mapNotNull { id -> manifest[id] }
                .map { href -> File(opfDir, Uri.decode(href)) }
                .filter { it.exists() }
        }

        private fun saveManifestCache(cacheFile: File, root: File, spineFiles: List<File>) {
            val relativePaths = spineFiles.map { it.relativeTo(root).path }
            cacheFile.writeText(relativePaths.joinToString("\n"))
        }
    }
}
