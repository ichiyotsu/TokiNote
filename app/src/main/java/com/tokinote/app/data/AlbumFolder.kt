package com.tokinote.app.data

/**
 * 相册文件夹：只保存图片路径，支持命名与创建时间。
 * 删除文件夹不会删除照片本身。
 */
data class AlbumFolder(
    val id: String,
    val name: String,
    val paths: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
)
