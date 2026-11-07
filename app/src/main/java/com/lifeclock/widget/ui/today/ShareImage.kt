package com.lifeclock.widget.ui.today

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The "share as image" picture: age, the life grid and the app name on a dark card. It carries no birth date, only the
 * age, so what leaves the phone is what the user chose to show.
 */
object ShareImage {
    const val WIDTH = 1080
    const val HEIGHT = 1350
    private val background = Color(0xFF0F0F14)
    private val ink = Color.White

    fun render(years: Int, share: Double, ageText: String, caption: String, footer: String): Bitmap {
        val image = ImageBitmap(WIDTH, HEIGHT)
        val scope = CanvasDrawScope()
        scope.draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(WIDTH.toFloat(), HEIGHT.toFloat())) {
            drawRect(background)
            val native = drawContext.canvas.nativeCanvas
            fun text(s: String, y: Float, sizePx: Float, alpha: Float, bold: Boolean) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ink.copy(alpha = alpha).toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = sizePx
                    typeface = Typeface.create("sans-serif-medium", if (bold) Typeface.BOLD else Typeface.NORMAL)
                }
                native.drawText(s, WIDTH / 2f, y, p)
            }
            text(caption, 150f, 44f, 0.6f, bold = false)
            text(ageText, 330f, 200f, 1f, bold = true)
            // The grid gets the middle of the card, with margins. inset shrinks the scope, and drawLifeGrid sizes to it.
            inset(left = 90f, top = 400f, right = 90f, bottom = HEIGHT - 400f - 780f) {
                drawLifeGrid(years, share, filled = ink, empty = ink.copy(alpha = 0.17f))
            }
            text(footer, HEIGHT - 80f, 40f, 0.5f, bold = false)
        }
        return image.asAndroidBitmap()
    }

    /** Writes the PNG into the cache and opens the system share sheet for it. Call from any thread. */
    suspend fun share(ctx: Context, bitmap: Bitmap, chooserTitle: String) {
        val uri = withContext(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "anicca-life.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(chooser)
    }
}
