package com.tokinote.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 正文字号：以默认字号的倍率表示。 */
enum class TextSize(val scale: Float, val titleScale: Float) {
    Small(0.88f, 0.85f),
    Medium(1f, 1f),
    Large(1.18f, 1.12f),
}

/** 正文字重。 */
enum class TextWeight(val weight: Int) {
    Normal(400),
    Medium(500),
    Bold(700),
}

/** 导入的文档（doc/docx/ppt/pptx/xls/xlsx/pdf 等）。 */
data class JournalFile(
    val name: String,
    val path: String,
    val mime: String,
    val size: Long,
)

/** 日记关联的位置，坐标使用 GCJ-02（腾讯地图坐标系）。 */
data class JournalLocation(
    val lat: Double,
    val lng: Double,
    val name: String,
)

/**
 * 一条日记。日期（day）决定它在时间线里的位置，emoji 是 June 式的情绪标记。
 */
data class Note(
    val id: String,
    val title: String = "",
    val body: String = "",
    val emoji: String = "😀",
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val done: Boolean = false,
    val labelIds: Set<String> = emptySet(),
    val titleSize: TextSize = TextSize.Medium,
    val bodySize: TextSize = TextSize.Medium,
    val bodyWeight: TextWeight = TextWeight.Normal,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val day: Long = 0L,
    val media: List<String> = emptyList(),
    val files: List<JournalFile> = emptyList(),
    val location: JournalLocation? = null,
) {
    val isBlank: Boolean
        get() = title.isBlank() && body.isBlank() && media.isEmpty() &&
            files.isEmpty() && location == null

    /** 列表里展示的预览文本。 */
    val preview: String get() = body.trim()

    /** 日记所属日期，老数据（day = 0）回退到创建时间。 */
    val journalDate: LocalDate
        get() = if (day != 0L) {
            LocalDate.ofEpochDay(day)
        } else {
            Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
}
