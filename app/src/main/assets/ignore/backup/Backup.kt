package ignore.backup

import android.content.Context
import android.net.Uri
import com.example.mybookhoard.data.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.io.copyTo
import kotlin.io.inputStream
import kotlin.io.outputStream
import kotlin.io.use

private fun files(ctx: android.content.Context): List<java.io.File> {
    val base = ctx.getDatabasePath(com.example.mybookhoard.data.AppDb.Companion.DB_NAME)
    return kotlin.collections.listOf(
        base,
        java.io.File(base.parent, "${com.example.mybookhoard.data.AppDb.Companion.DB_NAME}-wal"),
        java.io.File(base.parent, "${com.example.mybookhoard.data.AppDb.Companion.DB_NAME}-shm")
    ).filter { it.exists() }
}

suspend fun exportDbZip(ctx: android.content.Context, outUri: android.net.Uri) =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        com.example.mybookhoard.data.AppDb.Companion.closeDb()
        ctx.contentResolver.openOutputStream(outUri)?.use { out ->
            java.util.zip.ZipOutputStream(out).use { zip ->
                files(ctx).forEach { f ->
                    zip.putNextEntry(java.util.zip.ZipEntry(f.name))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

suspend fun importDbZip(ctx: android.content.Context, inUri: android.net.Uri) =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        com.example.mybookhoard.data.AppDb.Companion.closeDb()
        val dbDir =
            ctx.getDatabasePath(com.example.mybookhoard.data.AppDb.Companion.DB_NAME).parentFile!!
        ctx.contentResolver.openInputStream(inUri)?.use { inp ->
            java.util.zip.ZipInputStream(inp).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    val outFile = java.io.File(dbDir, e.name)
                    outFile.outputStream().use { zip.copyTo(it) }
                    zip.closeEntry()
                    e = zip.nextEntry
                }
            }
        }
    }
