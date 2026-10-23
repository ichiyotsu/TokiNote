package com.tokinote.app.data

/** 一个笔记标签（Google Keep 式的分组）。 */
data class Label(
    val id: String,
    val name: String,
    val colorIndex: Int = 0,
)
