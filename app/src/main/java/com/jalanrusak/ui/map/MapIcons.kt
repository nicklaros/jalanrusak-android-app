package com.jalanrusak.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.jalanrusak.R

/**
 * Draws and caches map marker icons (status pins, cluster count bubbles).
 * Sizes are density-corrected. Colors drawn over the map use fixed map_* resources
 * because map tiles are always light, even in night theme.
 */
object MapIcons {

    fun statusColor(context: Context, status: String): Int = ContextCompat.getColor(
        context,
        when (status) {
            "verified" -> R.color.status_verified
            "under_verification" -> R.color.status_under_verification
            "submitted" -> R.color.status_submitted
            "pending_resolved" -> R.color.status_pending_resolved
            "resolved" -> R.color.status_resolved
            "archived" -> R.color.status_archived
            else -> R.color.status_other
        }
    )

    private val pinCache = HashMap<String, BitmapDrawable>()
    private val clusterCache = HashMap<Int, BitmapDrawable>()

    /** Teardrop pin: ~36dp head, ~50dp tall incl. tip, 2dp white outline, white pupil. */
    fun pin(context: Context, status: String): BitmapDrawable =
        pinCache.getOrPut(status) { drawPin(context, statusColor(context, status)) }

    /** Count bubble: 44/52/60dp by count, white ring, bold count ("99+" beyond 99). */
    fun cluster(context: Context, count: Int): BitmapDrawable =
        clusterCache.getOrPut(count) { drawCluster(context, count) }

    fun clearCache() {
        pinCache.clear()
        clusterCache.clear()
    }

    private fun drawPin(context: Context, color: Int): BitmapDrawable {
        val d = context.resources.displayMetrics.density
        val w = 36f * d
        val h = 50f * d
        val stroke = 2f * d
        val pad = stroke
        val cx = pad + w / 2f
        val bmp = Bitmap.createBitmap((w + 2 * pad).toInt(), (h + 2 * pad).toInt(), Bitmap.Config.ARGB_8888)

        Canvas(bmp).apply {
            val head = Path().apply { addCircle(cx, pad + w / 2f, w / 2f - stroke / 2f, Path.Direction.CW) }
            val tail = Path().apply {
                moveTo(cx, pad + h - stroke / 2f) // tip
                lineTo(cx - w * 0.36f, pad + w * 0.62f)
                lineTo(cx + w * 0.36f, pad + w * 0.62f)
                close()
            }
            val pin = Path()
            pin.op(head, tail, Path.Op.UNION)

            // White silhouette first (outline), then colored fill, then white pupil
            drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = stroke
            })
            drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            })
            drawCircle(cx, pad + w * 0.5f, 3.8f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
            })
        }
        return wrap(bmp)
    }

    private fun drawCluster(context: Context, count: Int): BitmapDrawable {
        val d = context.resources.displayMetrics.density
        val diameter = (if (count < 10) 44f else if (count < 100) 52f else 60f) * d
        val ring = 2.5f * d
        val c = diameter / 2f
        val bmp = Bitmap.createBitmap(diameter.toInt(), diameter.toInt(), Bitmap.Config.ARGB_8888)

        Canvas(bmp).apply {
            drawCircle(c, c, c, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            drawCircle(c, c, c - ring, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.map_cluster_fill)
            })
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.map_icon_text)
                textSize = diameter * 0.38f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val text = if (count > 99) "99+" else count.toString()
            drawText(text, c, c - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        }
        return wrap(bmp)
    }

    private fun wrap(bmp: Bitmap) = BitmapDrawable(null, bmp).apply { setBounds(0, 0, bmp.width, bmp.height) }
}
