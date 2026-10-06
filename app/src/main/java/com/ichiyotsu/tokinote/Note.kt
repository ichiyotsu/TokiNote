package com.ichiyotsu.tokinote

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val tags: List<String>,
    val tone: Int,
    val favorite: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

enum class NoteFilter(val label: String) {
    ALL("全部笔记"),
    FAVORITES("我喜欢的"),
    ARCHIVE("归档")
}
