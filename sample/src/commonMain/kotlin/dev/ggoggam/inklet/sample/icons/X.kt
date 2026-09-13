// Adapted from https://raw.githubusercontent.com/composablehorizons/compose-icons/37d0e9fbe7f162c4b10df1eb302c983d30029454/icons-lucide-cmp/src/commonMain/kotlin/com/composables/icons/lucide/x.kt
// License: sample/src/commonMain/composeResources/files/lucide-LICENSE.txt

package dev.ggoggam.inklet.sample.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Lucide.X: ImageVector
    get() {
        if (cachedX != null) return cachedX!!

        cachedX =
            ImageVector
                .Builder(
                    name = "x",
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
                        moveTo(18f, 6f)
                        lineTo(6f, 18f)
                    }
                    path(
                        fill = SolidColor(Color.Transparent),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(6f, 6f)
                        lineToRelative(12f, 12f)
                    }
                }.build()

        return cachedX!!
    }

private var cachedX: ImageVector? = null
