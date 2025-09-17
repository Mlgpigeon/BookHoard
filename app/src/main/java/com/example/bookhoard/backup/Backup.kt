package com.example.bookhoard.backup

import android.content.Context
import android.net.Uri
import com.example.bookhoard.data.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun files(ctx: Context): List<File> {
    val base = ctx.getDatabasePath(AppDb.DB_NAME)
    return listOf(
        base,
        File(base.parent, "${AppDb.DB_NAME}-wal"),
        File(base.parent, "${AppDb.DB_NAME}-shm")
    ).filter { it.exists() }
}

suspend fun exportDbZip(ctx: Context, outUri: Uri) = withContext(Dispatchers.IO) {
    AppDb.closeDb()
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

suspend fun importDbZip(ctx: Context, inUri: Uri) = withContext(Dispatchers.IO) {
    AppDb.closeDb()
    val dbDir = ctx.getDatabasePath(AppDb.DB_NAME).parentFile!!
    ctx.contentResolver.openInputStream(inUri)?.use { inp ->
        java.util.zip.ZipInputStream(inp).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val outFile = File(dbDir, e.name)
                outFile.outputStream().use { zip.copyTo(it) }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
    }
}
