package dev.hridaya.kubenexus.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Github: ImageVector
    get() {
        if (_github != null) return _github!!
        _github = ImageVector.Builder(
            name = "Github",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.484f, 2f, 12.017f)
                curveTo(2f, 16.442f, 4.865f, 20.197f, 8.839f, 21.521f)
                curveTo(9.339f, 21.613f, 9.521f, 21.304f, 9.521f, 21.038f)
                curveTo(9.521f, 20.801f, 9.513f, 19.933f, 9.508f, 19.098f)
                curveTo(6.726f, 19.703f, 6.139f, 17.755f, 6.139f, 17.755f)
                curveTo(5.685f, 16.597f, 5.029f, 16.289f, 5.029f, 16.289f)
                curveTo(4.121f, 15.669f, 4.098f, 15.677f, 4.098f, 15.677f)
                curveTo(5.101f, 15.747f, 5.628f, 16.709f, 5.628f, 16.709f)
                curveTo(6.52f, 18.239f, 7.969f, 17.797f, 8.509f, 17.541f)
                curveTo(8.601f, 16.894f, 8.859f, 16.453f, 9.145f, 16.203f)
                curveTo(6.925f, 15.95f, 4.59f, 15.09f, 4.59f, 11.249f)
                curveTo(4.59f, 10.156f, 4.98f, 9.261f, 5.619f, 8.561f)
                curveTo(5.516f, 8.308f, 5.07f, 7.289f, 5.568f, 5.939f)
                curveTo(5.568f, 5.939f, 6.408f, 5.669f, 9.158f, 6.695f)
                curveTo(10.008f, 6.691f, 10.858f, 6.806f, 11.657f, 7.028f)
                curveTo(13.566f, 5.732f, 16.404f, 6.001f, 16.404f, 6.001f)
                curveTo(16.95f, 7.38f, 16.606f, 8.399f, 16.506f, 8.652f)
                curveTo(17.146f, 9.352f, 17.534f, 10.247f, 17.534f, 11.249f)
                curveTo(17.534f, 15.097f, 15.195f, 15.95f, 12.968f, 16.203f)
                curveTo(13.327f, 16.512f, 13.645f, 17.124f, 13.645f, 18.104f)
                curveTo(13.645f, 19.442f, 13.633f, 20.523f, 13.633f, 20.852f)
                curveTo(13.633f, 21.12f, 13.813f, 21.432f, 14.321f, 21.334f)
                curveTo(18.296f, 19.999f, 22f, 16.244f, 22f, 12.017f)
                curveTo(22f, 6.484f, 17.522f, 2f, 12f, 2f)
                close()
            }
        }.build()
        return _github!!
    }

private var _github: ImageVector? = null
