// Port of Drawably's src/prng.ts and src/rough.ts.
// Copyright (c) 2026 Daniel Belyi. MIT; see LICENSE.
package dev.ggoggam.inklet

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Coordinates are logical units (dp in Compose), independent of device density. */
data class PenPoint(
    val x: Double,
    val y: Double,
)

data class PenStroke(
    val points: List<PenPoint>,
    val closed: Boolean = false,
)

data class RoughOptions(
    val seed: Int,
    val roughness: Double = 1.0,
    val boil: Double = 0.3,
    val boilSeed: Int? = null,
) {
    init {
        require(roughness.isFinite() && roughness >= 0)
        require(boil.isFinite() && boil >= 0)
    }
}

/** Bit-for-bit equivalent to the upstream unsigned JavaScript PRNG, including overflow. */
class Mulberry32(
    seed: Int,
) {
    private var state = seed

    fun nextDouble(): Double {
        state += 0x6d2b79f5
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }
}

/** Native geometry, with upstream sampling, two pen passes, and midpoint smoothing. */
object Rough {
    fun sampleLine(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        step: Double = 8.0,
    ): List<PenPoint> {
        require(step.isFinite() && step > 0)
        val n = max(2, ceil(hypot(x2 - x1, y2 - y1) / step).toInt())
        return List(n + 1) { i -> PenPoint(x1 + (x2 - x1) * i / n, y1 + (y2 - y1) * i / n) }
    }

    private fun ellipsePoints(
        cx: Double,
        cy: Double,
        rx: Double,
        ry: Double,
        a0: Double,
        a1: Double,
        n: Int,
    ) = List(n + 1) { i ->
        val a = a0 + (a1 - a0) * i / n
        PenPoint(cx + rx * cos(a), cy + ry * sin(a))
    }

    private fun jitter(
        points: List<PenPoint>,
        rand: Mulberry32,
        amp: Double,
    ) = points.map {
        PenPoint(it.x + (rand.nextDouble() * 2 - 1) * amp, it.y + (rand.nextDouble() * 2 - 1) * amp)
    }

    private fun boil(
        points: List<PenPoint>,
        o: RoughOptions,
    ): List<PenPoint> = if (o.boil == 0.0 || o.boilSeed == null) points else jitter(points, Mulberry32(o.boilSeed), o.boil)

    private fun doubleStroke(
        points: List<PenPoint>,
        o: RoughOptions,
        close: Boolean,
    ): List<PenStroke> {
        val rand = Mulberry32(o.seed)
        return listOf(1.5, 2.1).map { amplitude ->
            PenStroke(boil(jitter(points, rand, amplitude * o.roughness), o), close)
        }
    }

    fun line(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        o: RoughOptions,
    ) = doubleStroke(sampleLine(x1, y1, x2, y2), o, false)

    fun roundedRect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        radius: Double,
        o: RoughOptions,
    ): List<PenStroke> {
        if (w <= 0 || h <= 0) return emptyList()
        val r = radius.coerceIn(0.0, min(w, h) / 2)

        fun arc(
            cx: Double,
            cy: Double,
            start: Double,
            end: Double,
        ) = ellipsePoints(cx, cy, r, r, start, end, 4)
        val points =
            sampleLine(x + r, y, x + w - r, y) +
                arc(x + w - r, y + r, -PI / 2, 0.0) +
                sampleLine(x + w, y + r, x + w, y + h - r) +
                arc(x + w - r, y + h - r, 0.0, PI / 2) +
                sampleLine(x + w - r, y + h, x + r, y + h) +
                arc(x + r, y + h - r, PI / 2, PI) +
                sampleLine(x, y + h - r, x, y + r) +
                arc(x + r, y + r, PI, PI * 1.5)
        return doubleStroke(points, o, true)
    }

    fun ellipse(
        cx: Double,
        cy: Double,
        rx: Double,
        ry: Double,
        o: RoughOptions,
    ): List<PenStroke> {
        if (rx <= 0 || ry <= 0) return emptyList()
        val ratio = (rx - ry) / (rx + ry)
        val h = ratio * ratio
        val perimeter = PI * (rx + ry) * (1 + 3 * h / (10 + sqrt(4 - 3 * h)))
        val n = max(8, ceil(perimeter / 8).toInt())
        return doubleStroke(ellipsePoints(cx, cy, rx, ry, 0.0, PI * 2, n).dropLast(1), o, true)
    }

    fun circle(
        cx: Double,
        cy: Double,
        r: Double,
        o: RoughOptions,
    ) = ellipse(cx, cy, r, r, o)

    fun checkmark(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        o: RoughOptions,
    ): List<PenStroke> {
        if (w <= 0 || h <= 0) return emptyList()
        // Perturb the gesture's anchors, not every sample: a wrist makes two flowing strokes.
        val anchors =
            jitter(
                listOf(
                    PenPoint(x, y + h * 0.48),
                    PenPoint(x + w * 0.16, y + h * 0.72),
                    PenPoint(x + w * 0.32, y + h * 0.92),
                    PenPoint(x + w * 0.63, y + h * 0.35),
                    PenPoint(x + w, y),
                ),
                Mulberry32(o.seed),
                min(w, h) * 0.055 * o.roughness,
            )
        return listOf(PenStroke(boil(anchors, o)))
    }

    /** A single softly irregular filled mark, without doubled outlines at dot scale. */
    fun dot(
        cx: Double,
        cy: Double,
        radius: Double,
        o: RoughOptions,
    ): List<PenStroke> {
        if (radius <= 0) return emptyList()
        val random = Mulberry32(o.seed)
        val phase = random.nextDouble() * 2 * PI
        val phase2 = random.nextDouble() * 2 * PI
        val amplitude = (o.roughness * 0.12).coerceAtMost(0.3)
        val points =
            List(20) { i ->
                val angle = i * 2 * PI / 20
                val r = radius * (1 + amplitude * (0.65 * sin(2 * angle + phase) + 0.35 * sin(3 * angle + phase2)))
                PenPoint(cx + r * cos(angle), cy + r * sin(angle))
            }
        return listOf(PenStroke(boil(points, o), closed = true))
    }

    fun arrow(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        o: RoughOptions,
    ): List<PenStroke> {
        val a = atan2(y2 - y1, x2 - x1)
        val rand = Mulberry32(o.seed)
        val head =
            listOf(PI / 6, -PI / 6).map { angle ->
                val points = sampleLine(x2, y2, x2 - 12 * cos(a + angle), y2 - 12 * sin(a + angle), 4.0)
                PenStroke(boil(jitter(points, rand, 1.2 * o.roughness), o))
            }
        return line(x1, y1, x2, y2, o) + head
    }

    fun scribble(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        o: RoughOptions,
    ): List<PenStroke> {
        if (w <= 0 || h <= 0) return emptyList()
        val points = mutableListOf<PenPoint>()
        var t = 6.0
        var flip = false
        while (t < w + h) {
            val a = PenPoint(x + max(0.0, t - h), y + min(t, h))
            val b = PenPoint(x + min(t, w), y + max(0.0, t - w))
            points += if (flip) listOf(b, a) else listOf(a, b)
            flip = !flip
            t += 6
        }
        return if (points.size < 2) {
            emptyList()
        } else {
            listOf(PenStroke(boil(jitter(points, Mulberry32(o.seed), 1.2 * o.roughness), o)))
        }
    }

    fun variants(
        o: RoughOptions,
        generate: (RoughOptions) -> List<PenStroke>,
    ): List<List<PenStroke>> = List(if (o.boil == 0.0) 1 else 3) { i -> generate(o.copy(boilSeed = o.seed + (i + 1) * 7919)) }
}
