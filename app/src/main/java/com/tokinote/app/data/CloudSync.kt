package com.tokinote.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/** 通过 SAF 访问 Google Drive / OneDrive 等云盘文件夹。 */
object CloudSync {

    private const val FILE_NAME = "tokinote-sync.json"

    fun write(context: Context, treeUri: Uri, content: String): Boolean = runCatching {
        val resolver = context.contentResolver
        val uri = findFile(context, treeUri) ?: DocumentsContract.createDocument(
            resolver,
            treeUri,
            "application/json",
            FILE_NAME,
        ) ?: return false
        resolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) } != null
    }.getOrDefault(false)

    fun read(context: Context, treeUri: Uri): String? = runCatching {
        val uri = findFile(context, treeUri) ?: return null
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    private fun findFile(context: Context, treeUri: Uri): Uri? {
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == FILE_NAME) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0))
                }
            }
        }
        return null
    }
}
