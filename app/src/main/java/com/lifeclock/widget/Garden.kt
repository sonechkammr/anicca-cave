package com.lifeclock.widget

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A raked sand garden that is different every day. The day number seeds the stone layout; every stone sends
 * out ripples, the ripples add up (they reinforce and cancel each other), and the sum is lit from the top left
 * like furrows in sand. No state, no network: the same date always draws the same garden.
 */
object Garden {
    /** Everything is laid out in a 252-unit-high space and scaled to the bitmap. */
    const val UNIT_HEIGHT = 252.0

    class Stone(
        val x: Double, val y: Double, val r: Double,
        val shapeA: Double, val shapeB: Double, val roughness: Double,
        val phase: Double, val wavelength: Double,
    ) {
        /** Irregular outline: the radius changes a little with the angle. */
        fun radiusToward(dx: Double, dy: Double): Double {
            val t = atan2(dy, dx)
            return r * (1 + roughness * sin(2 * t + shapeA) + 0.5 * roughness * sin(3 * t + shapeB))
        }
    }

    fun seed(epochDay: Long): Int = ((epochDay * 2654435761L) and 0xFFFFFFFFL).toInt()

    /** Mulberry32, a tiny seeded generator. */
    private class Random32(seed: Int) {
        private var a = seed
        fun next(): Double {
            a += 0x6D2B79F5
            var t = a
            t = (t xor (t ushr 15)) * (t or 1)
            t = (t + (t xor (t ushr 7)) * (t or 61)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296.0
        }
    }

    /** 5 to 7 stones, kept clear of each other and of the edges. [width] is in the same units as the height. */
    fun layout(epochDay: Long, width: Double): List<Stone> {
        val rnd = Random32(seed(epochDay))
        val count = 5 + (rnd.next() * 3).toInt()
        val stones = ArrayList<Stone>()
        var tries = 0
        while (stones.size < count && tries < 800) {
            tries++
            val rad = 10 + rnd.next() * rnd.next() * 22
            val x = 50 + rnd.next() * (width - 100)
            val y = 40 + rnd.next() * (UNIT_HEIGHT - 80)
            if (stones.all { hypot(it.x - x, it.y - y) > it.r + rad + 46 }) {
                stones.add(
                    Stone(
                        x, y, rad, rnd.next() * 6.28, rnd.next() * 6.28, 0.08 + rnd.next() * 0.1,
                        rnd.next() * 6.28, 12 + rnd.next() * 5,
                    ),
                )
            }
        }
        return stones
    }

    private const val LIGHT_X = -0.62
    private const val LIGHT_Y = -0.78

    private fun smooth(a: Double, b: Double, x: Double): Double {
        val t = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    private fun grain(x: Int, y: Int): Double {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL) / 4294967296.0
    }

    /** Brightness of a stone's surface at (x, y), lit from the top left: 0.38 in shade up to 1 in the highlight. */
    private fun stoneLight(s: Stone, x: Double, y: Double): Double {
        val rho = min(1.0, hypot(x - s.x, y - s.y) / (s.radiusToward(x - s.x, y - s.y) + 1e-6))
        val nz = sqrt(max(0.0, 1 - rho * rho))
        val nx = (x - s.x) / (s.r + 1e-6) * 0.8
        val ny = (y - s.y) / (s.r + 1e-6) * 0.8
        return min(1.0, 0.38 + 0.62 * max(0.0, nx * -0.5 + ny * -0.6 + nz * 0.62))
    }

    /**
     * ARGB pixels. With [natural] the garden is sand and grey stone. Otherwise it is white with varying alpha,
     * so the ImageView tint can give it the system (or chosen) colour.
     */
    fun pixels(w: Int, h: Int, stones: List<Stone>, natural: Boolean): IntArray {
        val u = h / UNIT_HEIGHT
        val out = IntArray(w * h)
        val twoPi = 2 * PI
        for (py in 0 until h) {
            val y = py / u
            for (px in 0 until w) {
                val x = px / u
                var edge = 1e9
                var nearest = stones[0]
                var height = 0.0
                var relief = 0.0
                var shadow = 0.0
                for (s in stones) {
                    val dx = x - s.x
                    val dy = y - s.y
                    val dist = hypot(dx, dy) + 1e-6
                    val e = dist - s.radiusToward(dx, dy)
                    if (e < edge) { edge = e; nearest = s }
                    val envelope = smooth(1.0, 16.0, e) * exp(-max(e, 0.0) / 95.0)
                    val phase = twoPi / s.wavelength * dist + s.phase
                    height += envelope * cos(phase)
                    relief += -envelope * sin(phase) * (dx / dist * LIGHT_X + dy / dist * LIGHT_Y)

                    val sx = dx - s.r * 0.55
                    val sy = dy - s.r * 0.7
                    val shadowEdge = hypot(sx, sy) - s.radiusToward(sx, sy) * 1.02
                    shadow = max(shadow, 0.32 * (1 - smooth(0.0, s.r * 0.9 + 8, shadowEdge)))
                }
                height *= 0.5
                relief *= 0.5
                val inside = smooth(-0.9, 0.9, -edge)             // 1 inside a stone, soft at the rim
                val contact = 1 - smooth(0.0, 3.0, max(edge, 0.0))

                if (natural) {
                    val n = (grain(px, py) - 0.5) * 5
                    val k = relief * 14 - height * 4
                    var r = 224 + n + k - shadow * 90 - contact * 30
                    var g = 211 + n + k * 0.96 - shadow * 96 - contact * 32
                    var b = 186 + n * 1.1 + k * 0.85 - shadow * 92 - contact * 30
                    if (inside > 0) {
                        val lt = stoneLight(nearest, x, y)
                        r += (52 + lt * 76 + n - r) * inside
                        g += (49 + lt * 72 + n - g) * inside
                        b += (46 + lt * 66 + n - b) * inside
                    }
                    out[py * w + px] = 0xFF shl 24 or (r.coerceIn(0.0, 255.0).toInt() shl 16) or
                        (g.coerceIn(0.0, 255.0).toInt() shl 8) or b.coerceIn(0.0, 255.0).toInt()
                } else {
                    var a = max(0.0, relief * 52 + height * 14) + shadow * 60 * (1 - smooth(0.0, 3.0, max(-edge, 0.0)))
                    if (inside > 0) a += (255 - stoneLight(nearest, x, y) * 90 - a) * inside
                    out[py * w + px] = (min(255.0, a).toInt() shl 24) or 0xFFFFFF
                }
            }
        }
        return out
    }
}