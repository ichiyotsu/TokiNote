package com.tokinote.app.data

/**
 * 首页分组：用户手动挑选的一组笔记。
 *
 * 分组只保存笔记 id，不复制笔记内容；笔记本身仍只有一份，
 * 所以编辑、归档或删除笔记时分组里的内容会随之变化。
 */
data class NoteGroup(
    val id: String,
    val name: String,
    val noteIds: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
)
