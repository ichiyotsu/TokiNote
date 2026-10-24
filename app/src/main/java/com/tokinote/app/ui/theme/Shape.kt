package com.tokinote.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Expressive corner scale: much rounder than the baseline M3 scale. */
val TokiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** 笔记卡片：四角不对称，避免通用圆角的“模板感”。 */
val ExpressiveCardShape = RoundedCornerShape(
    topStart = 34.dp,
    topEnd = 22.dp,
    bottomEnd = 34.dp,
    bottomStart = 22.dp,
)

/** 分组胶囊：一端更圆、一端收紧，像一片被压过的叶子。 */
val ExpressiveChipShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp,
    bottomStart = 7.dp,
)

/** Expressive FAB：略带动感的不对称容器。 */
val ExpressiveFabShape = RoundedCornerShape(
    topStart = 26.dp,
    topEnd = 26.dp,
    bottomEnd = 26.dp,
    bottomStart = 10.dp,
)

/** 编辑页悬浮工具岛 / 信息面板。 */
val ExpressiveIslandShape = RoundedCornerShape(34.dp)
val ExpressiveSheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
