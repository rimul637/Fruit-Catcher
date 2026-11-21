package com.fruitcatcher.fruit.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.fruitcatcher.fruit.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal enum class Scene { SPLASH, MENU, PLAY, FROZEN }

internal class SceneArt(context: Context) {
    private val resources = context.resources
    private fun bitmap(resource: Int) = requireNotNull(BitmapFactory.decodeResource(resources, resource))
    val orchard = bitmap(R.drawable.orchard)
    val basket = bitmap(R.drawable.basket)
    val leaf = bitmap(R.drawable.leaf)
    val drops = intArrayOf(
        R.drawable.apple, R.drawable.orange, R.drawable.banana, R.drawable.mango,
        R.drawable.strawberry, R.drawable.watermelon, R.drawable.pineapple, R.drawable.grapes,
        R.drawable.golden, R.drawable.bomb,
    ).map(::bitmap).toTypedArray()
}

internal class GameRenderer(context: Context, private val engine: GameEngine, private val art: SceneArt) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textSize = 21f
        setShadowLayer(4f, 0f, 2f, 0xff1b311c.toInt())
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, WORLD_HEIGHT, intArrayOf(
            0x5d123323, 0x001b411d, 0x001b411d, 0x6f142915,
        ), floatArrayOf(0f, 0.22f, 0.72f, 1f), Shader.TileMode.CLAMP)
    }
    private val haze = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(0x1cfff8d7, 0x09fff8d7, 0x00fff8d7),
            floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP)
    }
    private val rect = RectF()
    private val source = Rect()
    private var scale = 1f
    private var originX = 0f
    private var originY = 0f
    private var lastFrame = 0L
    private var atmosphere = 0f
    private var pointer = -1

    var scene = Scene.FROZEN
        set(value) {
            if (field == value) return
            field = value
            pointer = -1
            lastFrame = 0L
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        scale = min(w / WORLD_WIDTH, h / WORLD_HEIGHT).coerceAtLeast(0.01f)
        originX = (w - WORLD_WIDTH * scale) / 2
        originY = (h - WORLD_HEIGHT * scale) / 2
        val ratio = w.toFloat() / h.coerceAtLeast(1)
        val cropW = min(art.orchard.width, (art.orchard.height * ratio).toInt()).coerceAtLeast(1)
        val cropH = min(art.orchard.height, (art.orchard.width / ratio).toInt()).coerceAtLeast(1)
        val left = (art.orchard.width - cropW) / 2
        val top = (art.orchard.height - cropH) / 2
        source.set(left, top, left + cropW, top + cropH)
    }

    override fun onDraw(canvas: Canvas) {
        val animated = scene != Scene.FROZEN
        if (animated) {
            val now = System.nanoTime()
            val seconds = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
            lastFrame = now
            atmosphere = (atmosphere + seconds) % 1200f
            if (scene == Scene.PLAY) engine.advance(seconds)
        }

        paint.alpha = 255
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(art.orchard, source, rect, paint)
        canvas.save()
        canvas.translate(originX, originY)
        canvas.scale(scale, scale)
        canvas.drawRect(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT, backgroundPaint)
        drawAtmosphere(canvas)
        when (scene) {
            Scene.PLAY, Scene.FROZEN -> drawGame(canvas)
            Scene.MENU, Scene.SPLASH -> drawMenu(canvas)
        }
        canvas.restore()
        if (animated && windowVisibility == VISIBLE) postInvalidateOnAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        lastFrame = 0L
        if (visibility == VISIBLE) invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scene != Scene.PLAY) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointer = event.getPointerId(0)
                engine.aim((event.x - originX) / scale)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(pointer)
                if (index >= 0) engine.aim((event.getX(index) - originX) / scale)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == pointer) {
                    val next = if (event.actionIndex == 0) 1 else 0
                    pointer = event.getPointerId(next)
                    engine.aim((event.getX(next) - originX) / scale)
                }
            }
            MotionEvent.ACTION_UP -> {
                pointer = -1
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> pointer = -1
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawAtmosphere(canvas: Canvas) {
        for (i in 0..2) {
            canvas.save()
            canvas.translate(32f + i * 159f + sin(atmosphere * 0.13f + i) * 37f, 92f + i * 49f)
            canvas.scale(76f + i * 8f, 18f + i * 3f)
            canvas.drawCircle(0f, 0f, 1f, haze)
            canvas.restore()
        }
        canvas.save()
        canvas.translate(180f + cos(atmosphere * 0.23f) * 75f, 525f)
        canvas.scale(135f, 36f)
        canvas.drawCircle(0f, 0f, 1f, haze)
        canvas.restore()
        paint.color = 0x14fff5c4
        for (i in 0..9) {
            val x = i * 42f + 12f + sin(atmosphere * 0.5f + i) * 9f
            val y = 373f + i % 3 * 57f + cos(atmosphere * 0.37f + i) * 21f
            canvas.drawCircle(x, y, 2f + i % 3, paint)
        }
        for (i in 0..6) {
            val y = (i * 106f + atmosphere * (8f + i)) % (WORLD_HEIGHT + 40f) - 20f
            val x = i * 57f + sin(atmosphere * 0.43f + i) * 21f
            sprite(canvas, art.leaf, x, y, 10f + i % 3 * 4f, 15f, atmosphere * 17f + i * 52f, 119)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        val dropCount = if (scene == Scene.SPLASH) 9 else 7
        for (i in 0 until dropCount) {
            val type = Drop.entries[i]
            val y = if (scene == Scene.SPLASH) {
                (i * 76f + atmosphere * (108f + i * 10f)) % 700f - 35f
            } else {
                269f + (i % 3) * 29f + sin(atmosphere * 1.65f + i * 1.9f) * 16f
            }
            val x = 30f + i * (300f / (dropCount - 1)) + cos(atmosphere * 0.85f + i) * 11f
            sprite(canvas, art.drops[i], x, y, type.width * 1.18f, type.height * 1.18f,
                cos(atmosphere * 1.2f + i * 2f) * 24f)
        }
        if (scene == Scene.MENU) sprite(canvas, art.basket, 180f, 361f, 170f, 99f, 0f)
    }

    private fun drawGame(canvas: Canvas) {
        for (item in engine.drops) {
            if (!item.active) {
                continue
            }
            val type = item.type
            if (type == Drop.GOLDEN) {
                paint.color = 0x40ffd562
                canvas.drawCircle(item.x, item.y, 28f + sin(item.phase * 2f) * 3f, paint)
            }
            sprite(canvas, art.drops[type.ordinal], item.x, item.y, type.width, type.height, item.angle)
            if (type == Drop.BOMB) {
                canvas.save()
                canvas.rotate(item.angle, item.x, item.y)
                val flicker = 2.5f + cos(item.phase * 28f) * 1.5f
                paint.color = 0x70ff7b2b
                canvas.drawCircle(item.x + 10f, item.y - 23f, flicker + 3.5f, paint)
                paint.color = 0xffffd759.toInt()
                canvas.drawCircle(item.x + 10f, item.y - 23f, flicker, paint)
                canvas.restore()
            }
        }

        val bounce = sin(engine.reaction * Math.PI.toFloat()) * 5f
        rect.set(engine.basket - 55f, BASKET_Y + 48f, engine.basket + 55f, BASKET_Y + 61f)
        paint.color = 0x60102512
        canvas.drawOval(rect, paint)
        sprite(canvas, art.basket, engine.basket, BASKET_Y + 23f + bounce,
            118f + bounce, 72f - bounce, 0f)

        for (spark in engine.sparks) {
            if (spark.life <= 0f) {
                continue
            }
            paint.color = spark.tint
            paint.alpha = (spark.life * 390).toInt().coerceAtMost(255)
            canvas.drawCircle(spark.x, spark.y, spark.radius, paint)
        }
        for (popup in engine.popups) {
            if (popup.life <= 0f) {
                continue
            }
            text.color = popup.tint
            text.alpha = (popup.life * 570).toInt().coerceAtMost(255)
            canvas.drawText(popup.label, popup.x, popup.y, text)
        }
        if (engine.flash > 0f) {
            paint.color = Color.argb((engine.flash * 62).toInt(), 255, 74, 38)
            canvas.drawRect(0f, BASKET_Y - 90f, WORLD_WIDTH, WORLD_HEIGHT, paint)
        }
        paint.alpha = 255
    }

    private fun sprite(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, width: Float, height: Float,
                       degrees: Float, opacity: Int = 255) {
        val factor = min(width / bitmap.width, height / bitmap.height)
        val halfWidth = bitmap.width * factor / 2
        val halfHeight = bitmap.height * factor / 2
        rect.set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)
        paint.alpha = opacity
        canvas.save()
        canvas.rotate(degrees, x, y)
        canvas.drawBitmap(bitmap, null, rect, paint)
        canvas.restore()
        paint.alpha = 255
    }
}
