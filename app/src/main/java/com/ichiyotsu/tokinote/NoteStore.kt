package com.ichiyotsu.tokinote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.tokiNoteDataStore by preferencesDataStore(name = "tokinote_notes")
private val NOTES_KEY = stringPreferencesKey("notes_json")

suspend fun loadNotes(context: Context): List<Note> {
    val saved = context.tokiNoteDataStore.data.first()[NOTES_KEY]
    return if (saved.isNullOrBlank()) starterNotes() else decodeNotes(saved)
}

suspend fun saveNotes(context: Context, notes: List<Note>) {
    context.tokiNoteDataStore.edit { preferences ->
        preferences[NOTES_KEY] = encodeNotes(notes)
    }
}

private fun encodeNotes(notes: List<Note>): String = JSONArray().apply {
    notes.forEach { note ->
        put(
            JSONObject()
                .put("id", note.id)
                .put("title", note.title)
                .put("body", note.body)
                .put("tags", JSONArray(note.tags))
                .put("tone", note.tone)
                .put("favorite", note.favorite)
                .put("archived", note.archived)
                .put("createdAt", note.createdAt)
                .put("updatedAt", note.updatedAt)
        )
    }
}.toString()

private fun decodeNotes(json: String): List<Note> = runCatching {
    val array = JSONArray(json)
    List(array.length()) { index ->
        val item = array.getJSONObject(index)
        val tagArray = item.optJSONArray("tags") ?: JSONArray()
        val tags = List(tagArray.length()) { tagIndex -> tagArray.optString(tagIndex) }
        Note(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = item.optString("title"),
            body = item.optString("body"),
            tags = tags,
            tone = item.optInt("tone").coerceIn(0, 4),
            favorite = item.optBoolean("favorite"),
            archived = item.optBoolean("archived"),
            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}.getOrElse { starterNotes() }

private fun starterNotes(): List<Note> {
    val now = System.currentTimeMillis()
    return listOf(
        Note(
            id = "welcome-thought", title = "给自己留一点空白",
            body = "不是每个念头都需要马上变成计划。\n\n先把它写下来，留一点空白给好奇心。等某一天回头看，也许会发现它早就在悄悄长大。\n\n今天也慢一点，没关系。",
            tags = listOf("随手记", "灵感"), tone = 0, favorite = true, archived = false,
            createdAt = now - 49 * 60 * 60 * 1000L, updatedAt = now - 2 * 60 * 60 * 1000L
        ),
        Note(
            id = "weekend-sea", title = "周末，去看看海吧",
            body = "带上相机和那本读了一半的书。\n\n想找一家临海的小咖啡店，坐到太阳开始变成橘色。手机少看一点，把风声听清楚。\n\n清单：\n· 防晒霜\n· 耳机\n· 一颗好心情",
            tags = listOf("生活", "小计划"), tone = 1, favorite = false, archived = false,
            createdAt = now - 80 * 60 * 60 * 1000L, updatedAt = now - 18 * 60 * 60 * 1000L
        ),
        Note(
            id = "slow-idea", title = "一个关于慢下来的小想法",
            body = "如果每天只记住一件小事，会不会更容易看见生活？\n\n比如路边新开的花，比如朋友说话时眼睛里的光。\n\n可以做成一个很简单的日记：每天一张照片，一句话。",
            tags = listOf("灵感", "写作"), tone = 2, favorite = false, archived = false,
            createdAt = now - 112 * 60 * 60 * 1000L, updatedAt = now - 31 * 60 * 60 * 1000L
        ),
        Note(
            id = "reading-attention", title = "读书摘记 · 关于注意力",
            body = "注意力不是被动分配出去的东西，而是一种可以练习的选择。\n\n把最重要的一件事放在最前面，剩下的事也就没那么急了。\n\n— 留给下次继续读",
            tags = listOf("阅读", "随手记"), tone = 3, favorite = false, archived = false,
            createdAt = now - 170 * 60 * 60 * 1000L, updatedAt = now - 54 * 60 * 60 * 1000L
        )
    )
}
