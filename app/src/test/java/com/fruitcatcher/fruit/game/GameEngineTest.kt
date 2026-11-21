package com.fruitcatcher.fruit.game

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun engine(seed: Int = 17) = GameEngine(Random(seed)).also { it.restart() }

    @Test fun sweptCollisionAwardsAllFruitValuesOnce() {
        val game = engine()
        val expected = Drop.entries.filter { it != Drop.BOMB }
        var score = 0L
        for (kind in expected) {
            inject(game, kind)
            game.advance(1f / 60)
            score += kind.points
            assertEquals(score, game.score)
            assertEquals(3, game.hearts)
            assertFalse(game.drops[0].active)
        }
    }

    @Test fun bombsCostOneHeartAndStopAtZero() {
        val game = engine()
        repeat(3) {
            inject(game, Drop.BOMB)
            game.advance(1f / 60)
            assertEquals(2 - it, game.hearts)
            game.advance(1f / 60)
            assertEquals(2 - it, game.hearts)
        }
        val time = game.elapsed
        game.advance(0.05f)
        game.resume()
        assertFalse(game.running)
        assertEquals(time, game.elapsed, 0f)
        assertEquals(0L, game.score)
        game.restart()
        assertTrue(game.running)
        assertEquals(3, game.hearts)
        assertEquals(0f, game.elapsed, 0f)
        assertTrue(game.drops.none { it.active })
    }

    @Test fun missingFruitOrBombDoesNotCostHealth() {
        val game = engine()
        inject(game, Drop.BOMB, 38f)
        repeat(12) { game.advance(1f / 60) }
        inject(game, Drop.APPLE, 320f)
        repeat(12) { game.advance(1f / 60) }
        assertEquals(3, game.hearts)
        assertEquals(0L, game.score)
        assertFalse(game.drops[0].active)
    }

    @Test fun pausePreservesTimersAndPositionsUntilResume() {
        val game = engine()
        repeat(55) { game.advance(1f / 60) }
        val drop = game.drops.first { it.active }
        val previousY = drop.y
        val previousTime = game.elapsed
        game.pause()
        game.aim(100f)
        repeat(300) { game.advance(1f / 60) }
        assertEquals(previousY, drop.y, 0f)
        assertEquals(previousTime, game.elapsed, 0f)
        assertEquals(180f, game.basket, 0f)
        game.resume()
        game.advance(1f / 60)
        assertTrue(drop.y > previousY)
    }

    @Test fun renderRatesProduceSamePhysicsWithinOneStep() {
        val games = listOf(60, 90, 120).map { rate ->
            engine().also { game -> repeat(rate * 6) { game.advance(1f / rate) } }
        }
        val baseline = games.first()
        for (game in games.drop(1)) {
            assertEquals(baseline.elapsed, game.elapsed, 0.009f)
            assertEquals(baseline.score, game.score)
            assertEquals(baseline.drops.count { it.active }, game.drops.count { it.active })
            baseline.drops.zip(game.drops).filter { it.first.active }.forEach { (first, second) ->
                assertEquals(first.type, second.type)
                assertEquals(first.y, second.y, 4.5f)
            }
        }
    }

    @Test fun clampsBasketAndRespondsToQuickDirectionChanges() {
        val game = engine()
        game.aim(-500f)
        game.advance(1f / 60)
        assertTrue(game.basket < 90f)
        repeat(20) { game.advance(1f / 60) }
        assertEquals(59f, game.basket, .001f)
        game.aim(900f)
        game.advance(1f / 60)
        assertTrue(game.basket > 240f)
        repeat(20) { game.advance(1f / 60) }
        assertEquals(301f, game.basket, .001f)
    }

    @Test fun extendedSimulationSpawnsEveryTypeAndReusesPool() {
        val game = engine(93)
        val named = mutableSetOf<Drop>()
        val firstGeneration = game.drops.toList()
        var latePeak = 0
        val step = 1f / 120
        repeat(120 * 3600) {
            val dangerous = game.drops.firstOrNull { drop ->
                drop.active && drop.type == Drop.BOMB && drop.y in 420f..560f
            }
            game.aim(if (dangerous != null && dangerous.x >= 180f) 59f else 301f)
            game.advance(step)
            game.drops.forEach { drop -> if (drop.active) named += drop.type }
            if (it > 120 * 120) latePeak = maxOf(latePeak, game.drops.count { drop -> drop.active })
        }
        assertTrue(game.running)
        assertEquals(Drop.entries.toSet(), named)
        assertTrue(latePeak in 3..8)
        game.drops.forEachIndexed { index, drop -> assertSame(firstGeneration[index], drop) }
    }

    private fun inject(game: GameEngine, kind: Drop, x: Float = 180f) {
        game.drops[0].apply {
            type = kind
            this.x = x
            y = BASKET_Y - type.height * .28f - 0.5f
            velocity = 900f
            gravity = 0f
            drift = 0f
            active = true
        }
    }
}
