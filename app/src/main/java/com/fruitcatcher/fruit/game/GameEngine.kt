package com.fruitcatcher.fruit.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class Drop(val points: Int, val width: Float, val height: Float, val tint: Int) {
    APPLE(10, 48f, 52f, 0xffff5455.toInt()),
    ORANGE(10, 46f, 49f, 0xffffa641.toInt()),
    BANANA(15, 55f, 52f, 0xffffe365.toInt()),
    MANGO(20, 49f, 54f, 0xffffb733.toInt()),
    STRAWBERRY(15, 45f, 53f, 0xffff6270.toInt()),
    WATERMELON(25, 56f, 45f, 0xfff55b67.toInt()),
    PINEAPPLE(25, 42f, 61f, 0xffffcc48.toInt()),
    GRAPES(15, 46f, 55f, 0xffba7ff4.toInt()),
    GOLDEN(100, 51f, 53f, 0xffffe58a.toInt()),
    BOMB(0, 51f, 53f, 0xffff9462.toInt())
}

internal const val WORLD_WIDTH = 360f
internal const val WORLD_HEIGHT = 640f
internal const val BASKET_Y = 549f
internal const val BASKET_HALF_WIDTH = 55f
private const val STEP = 1f / 120f

internal class Falling {
    var active = false
    var type = Drop.APPLE
    var x = 0f
    var y = 0f
    var velocity = 0f
    var gravity = 0f
    var phase = 0f
    var drift = 0f
    var angle = 0f
    var rotation = 0f
}

internal class Spark {
    var life = 0f
    var x = 0f
    var y = 0f
    var dx = 0f
    var dy = 0f
    var radius = 0f
    var tint = 0
}

internal class Popup {
    var life = 0f
    var x = 0f
    var y = 0f
    var label = ""
    var tint = 0
}

internal class GameEngine(
    private val random: Random = Random.Default,
    private val onCatch: (Drop) -> Unit = {},
) {
    val drops = Array(20) { Falling() }
    val sparks = Array(100) { Spark() }
    val popups = Array(12) { Popup() }
    private val points = Drop.entries.map { "+${it.points}" }

    var basket = WORLD_WIDTH / 2
        private set
    var score = 0L
        private set
    var hearts = 3
        private set
    var elapsed = 0f
        private set
    var reaction = 0f
        private set
    var flash = 0f
        private set
    var running = false
        private set
    private var target = basket
    private var accumulator = 0f
    private var untilSpawn = 0.42f
    private var untilBomb = 6f
    private var lastGolden = 0f
    private var lastLane = -1

    fun restart() {
        drops.forEach { it.active = false }
        sparks.forEach { it.life = 0f }
        popups.forEach { it.life = 0f }
        score = 0
        hearts = 3
        elapsed = 0f
        reaction = 0f
        flash = 0f
        basket = WORLD_WIDTH / 2f
        target = basket
        accumulator = 0f
        untilSpawn = 0.42f
        untilBomb = 6f
        lastGolden = 0f
        lastLane = -1
        running = true
    }

    fun pause() {
        running = false
    }

    fun resume() {
        running = hearts > 0
    }

    fun aim(x: Float) {
        if (running) target = x.coerceIn(BASKET_HALF_WIDTH + 4f, WORLD_WIDTH - BASKET_HALF_WIDTH - 4f)
    }

    fun advance(seconds: Float) {
        if (!running) return
        accumulator += seconds.coerceIn(0f, 0.05f)
        while (accumulator >= STEP && running) {
            accumulator -= STEP
            update()
        }
    }

    private fun update() {
        val previousBasket = basket
        basket += (target - basket) * 0.55f
        if (abs(target - basket) < 0.1f) basket = target
        elapsed += STEP
        reaction = (reaction - STEP * 4.2f).coerceAtLeast(0f)
        flash = (flash - STEP * 2.8f).coerceAtLeast(0f)
        untilSpawn -= STEP
        if (untilSpawn <= 0f) spawn()

        for (drop in drops) {
            if (!drop.active) continue
            val oldX = drop.x
            val oldEdge = drop.y + drop.type.height * 0.28f
            drop.velocity += drop.gravity * STEP
            drop.y += drop.velocity * STEP
            drop.phase += STEP * 2.4f
            drop.x += sin(drop.phase) * drop.drift * STEP
            drop.angle += drop.rotation * STEP
            val edge = drop.y + drop.type.height * 0.28f
            if (oldEdge < BASKET_Y && edge >= BASKET_Y) {
                val crossing = (BASKET_Y - oldEdge) / (edge - oldEdge)
                val center = previousBasket + (basket - previousBasket) * crossing
                val x = oldX + (drop.x - oldX) * crossing
                val reach = if (drop.type == Drop.BOMB) 39f else 49f
                if (abs(x - center) < reach + drop.type.width * 0.14f) {
                    collect(drop)
                    if (!running) break
                }
            }
            if (drop.y > WORLD_HEIGHT + 50f) drop.active = false
        }

        for (spark in sparks) {
            if (spark.life <= 0f) continue
            spark.life -= STEP
            spark.x += spark.dx * STEP
            spark.y += spark.dy * STEP
            spark.dy += 235f * STEP
        }
        for (popup in popups) {
            if (popup.life <= 0f) continue
            popup.life -= STEP
            popup.y -= 68f * STEP
        }
    }

    private fun spawn() {
        val difficulty = (elapsed / 115f).coerceIn(0f, 1f)
        untilSpawn = (0.96f - difficulty * 0.46f) * (0.88f + random.nextFloat() * 0.24f)
        val drop = drops.firstOrNull { !it.active } ?: return
        val lane = if (lastLane < 0) random.nextInt(6) else (lastLane + random.nextInt(1, 6)) % 6
        lastLane = lane
        val roll = random.nextFloat()
        val type = when {
            elapsed > 10f && elapsed - lastGolden > 12f && (roll < 0.09f || elapsed - lastGolden > 25f) -> {
                lastGolden = elapsed
                Drop.GOLDEN
            }
            elapsed >= untilBomb && roll < 0.12f + 0.20f * difficulty -> {
                untilBomb = elapsed + 3.8f - 1.9f * difficulty
                Drop.BOMB
            }
            else -> Drop.entries[random.nextInt(8)]
        }
        drop.type = type
        drop.x = 36f + lane * 57.6f + (random.nextFloat() - 0.5f) * 12f
        drop.y = -35f
        drop.velocity = 83f + difficulty * 80f
        drop.gravity = 138f + 139f * difficulty
        drop.phase = random.nextFloat() * 6.28f
        drop.drift = 5f + random.nextFloat() * 15f
        drop.angle = random.nextFloat() * 28f - 14f
        drop.rotation = random.nextFloat() * 65f - 32.5f
        drop.active = true
    }

    private fun collect(drop: Falling) {
        drop.active = false
        val type = drop.type
        val bomb = type == Drop.BOMB
        if (bomb) {
            hearts--
            flash = 1f
        } else {
            score += type.points
        }
        reaction = 1f
        burst(drop.x, type)
        popups.firstOrNull { it.life <= 0f }?.apply {
            life = 0.82f
            x = drop.x.coerceIn(42f, WORLD_WIDTH - 42f)
            y = BASKET_Y - 14f
            label = if (bomb) "−1 heart" else points[type.ordinal]
            tint = type.tint
        }
        if (hearts == 0) running = false
        onCatch(type)
    }

    private fun burst(x: Float, type: Drop) {
        var remaining = when (type) {
            Drop.BOMB -> 22
            Drop.GOLDEN -> 18
            else -> 10
        }
        for (spark in sparks) {
            if (remaining == 0) break
            if (spark.life > 0f) continue
            val angle = random.nextFloat() * 6.28f
            val speed = 35f + random.nextFloat() * 185f
            with(spark) {
                this.x = x
                y = BASKET_Y
                dx = cos(angle) * speed
                dy = sin(angle) * speed - 62f
                radius = 2f + random.nextFloat() * 4f
                tint = if (remaining % 3 == 0) 0xfffff6ce.toInt() else type.tint
                life = min(0.75f, 0.32f + random.nextFloat() * 0.45f)
            }
            remaining--
        }
    }
}
