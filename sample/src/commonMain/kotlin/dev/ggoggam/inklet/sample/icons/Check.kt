// Adapted from https://raw.githubusercontent.com/composablehorizons/compose-icons/37d0e9fbe7f162c4b10df1eb302c983d30029454/icons-lucide-cmp/src/commonMain/kotlin/com/composables/icons/lucide/check.kt
// License: sample/src/commonMain/composeResources/files/lucide-LICENSE.txt

package dev.ggoggam.inklet.sample.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Lucide.Check: ImageVector
    get() {
        if (cachedCheck != null) return cachedCheck!!

        cachedCheck =
            ImageVector
                .Builder(
                    name = "check",
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
                        moveTo(20f, 6f)
                        lineTo(9f, 17f)
                        lineToRelative(-5f, -5f)
                    }
                }.build()

        return cachedCheck!!
    }

private var cachedCheck: ImageVector? = null
