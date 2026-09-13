// Adapted from https://raw.githubusercontent.com/composablehorizons/compose-icons/37d0e9fbe7f162c4b10df1eb302c983d30029454/icons-lucide-cmp/src/commonMain/kotlin/com/composables/icons/lucide/arrow_up_right.kt
// License: sample/src/commonMain/composeResources/files/lucide-LICENSE.txt

package dev.ggoggam.inklet.sample.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Lucide.ArrowUpRight: ImageVector
    get() {
        if (cachedArrowUpRight != null) return cachedArrowUpRight!!

        cachedArrowUpRight =
            ImageVector
                .Builder(
                    name = "arrow-up-right",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    path(
                        fill = SolidColor(Color.Transparent),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(7f, 7f)
                        horizontalLineToRelative(10f)
                        verticalLineToRelative(10f)
                    }
                    path(
                        fill = SolidColor(Color.Transparent),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(7f, 17f)
                        lineTo(17f, 7f)
                    }
                }.build()

        return cachedArrowUpRight!!
    }

private var cachedArrowUpRight: ImageVector? = null
