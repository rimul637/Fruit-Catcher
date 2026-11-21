package com.fruitcatcher.fruit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fruitcatcher.fruit.MainActivity
import com.fruitcatcher.fruit.Screen
import com.fruitcatcher.fruit.game.GameRenderer
import com.fruitcatcher.fruit.game.Scene
import com.fruitcatcher.fruit.game.SceneArt

private val Cream = Color(0xfffff4ce)
private val Gold = Color(0xffffcf6c)
private val Moss = Color(0xff183723)
private val DarkMoss = Color(0xff102d22)
private val SoftGreen = Color(0xffd9efb6)
private val CardShape = RoundedCornerShape(25.dp)
private val ButtonShape = RoundedCornerShape(17.dp)
private val GameFont = FontFamily.Serif

@Composable
internal fun FruitCatcher(activity: MainActivity, art: SceneArt?, foreground: Boolean) {
    val screen = activity.screen
    val fromGame = screen == Screen.PAUSE || screen == Screen.GAME_OVER ||
        (screen == Screen.SETTINGS && activity.settingsFrom == Screen.PAUSE)
    val scene = when {
        !foreground || fromGame -> Scene.FROZEN
        screen == Screen.SPLASH -> Scene.SPLASH
        screen == Screen.PLAY -> Scene.PLAY
        else -> Scene.MENU
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, surface = Moss, onSurface = Cream)) {
        BackHandler(screen != Screen.MENU) { activity.back() }
        Box(Modifier.fillMaxSize().background(DarkMoss)) {
            if (art != null) {
                val context = LocalContext.current
                AndroidView(factory = { GameRenderer(context, activity.engine, art) },
                    modifier = Modifier.fillMaxSize(), update = { it.scene = scene })
            }
            when (screen) {
                Screen.SPLASH -> Splash()
                Screen.MENU -> MainMenu(activity)
                Screen.PLAY, Screen.PAUSE, Screen.GAME_OVER -> {
                    if (screen != Screen.GAME_OVER) Hud(activity, screen == Screen.PLAY)
                    when (screen) {
                        Screen.PAUSE -> Overlay("PAUSED") {
                            GameButton("Resume", true) { activity.navigate(Screen.PLAY) }
                            GameButton("Restart") { activity.start() }
                            GameButton("Settings") { activity.navigate(Screen.SETTINGS) }
                            GameButton("Main Menu") { activity.navigate(Screen.MENU) }
                        }
                        Screen.GAME_OVER -> Overlay("GAME OVER") {
                            if (activity.newRecord) {
                                Text("NEW PERSONAL RECORD", color = Gold, fontSize = 13.sp,
                                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                            ScorePair(activity.currentScore, activity.best)
                            GameButton("Restart", true) { activity.start() }
                            GameButton("Main Menu") { activity.navigate(Screen.MENU) }
                        }
                        else -> Unit
                    }
                }
                Screen.BEST -> Overlay("BEST SCORE") {
                    RecordNumber(activity.best)
                    GameButton("Main Menu", true) { activity.navigate(Screen.MENU) }
                }
                Screen.SETTINGS -> Overlay("SETTINGS") {
                    SoundSwitch("Background music", activity.options.music, activity::toggleMusic)
                    SoundSwitch("Sound effects", activity.options.effects, activity::toggleEffects)
                    Spacer(Modifier.height(4.dp))
                    GameButton("Back", true) { activity.navigate(activity.settingsFrom) }
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    val entrance = rememberEntrance()
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.graphicsLayer {
                alpha = entrance.value
                scaleX = 0.89f + entrance.value * 0.11f
                scaleY = scaleX
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo()
            Spacer(Modifier.height(30.dp))
            Box(Modifier.width(96.dp).height(5.dp).clip(ButtonShape).background(DarkMoss.copy(alpha = .8f))) {
                Box(Modifier.fillMaxWidth(entrance.value.coerceIn(0f, 1f)).fillMaxHeight().background(Gold))
            }
        }
    }
}

@Composable
private fun MainMenu(activity: MainActivity) {
    val entrance = rememberEntrance()
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        val compact = maxHeight < 590.dp
        Box(Modifier.align(Alignment.TopCenter).padding(top = maxHeight * if (compact) .09f else .13f)) {
            Logo(if (compact) 42 else 54)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 23.dp)
                .padding(bottom = if (compact) 18.dp else 38.dp).widthIn(max = 330.dp).fillMaxWidth()
                .graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 46f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameButton("Start Game", true, compact) { activity.start() }
            GameButton("Best Score", compact = compact) { activity.navigate(Screen.BEST) }
            GameButton("Settings", compact = compact) { activity.navigate(Screen.SETTINGS) }
        }
    }
}

@Composable
private fun Logo(size: Int = 54) {
    Text("FRUIT\nCATCHER", textAlign = TextAlign.Center, style = TextStyle(
        color = Cream,
        fontFamily = GameFont,
        fontWeight = FontWeight.Black,
        fontSize = size.sp,
        lineHeight = (size - 1).sp,
        letterSpacing = 1.sp,
        shadow = Shadow(Color(0xff503010), Offset(2f, 5f), 12f),
    ))
}

@Composable
private fun Hud(activity: MainActivity, canPause: Boolean) {
    Box(
        Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.align(Alignment.TopStart).height(48.dp)
                .clip(ButtonShape).background(DarkMoss.copy(alpha = .88f)).padding(horizontal = 8.dp)
                .semantics { contentDescription = "${activity.hearts} of 3 hearts" },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { Heart(it < activity.hearts) }
        }
        val pulse = remember { Animatable(1f) }
        LaunchedEffect(activity.currentScore) {
            if (activity.currentScore > 0) {
                pulse.snapTo(1.17f)
                pulse.animateTo(1f, tween(230))
            }
        }
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(min = 76.dp).clip(ButtonShape)
                .background(DarkMoss.copy(alpha = .9f)).padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("SCORE", color = SoftGreen, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text(activity.currentScore.toString(), color = Cream, fontSize = 23.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value })
        }
        if (canPause) {
            Box(
                Modifier.align(Alignment.TopEnd).size(48.dp).clip(ButtonShape)
                    .background(Moss).border(1.dp, Gold.copy(alpha = .65f), ButtonShape)
                    .semantics { contentDescription = "Pause" }
                    .clickable(role = Role.Button) { activity.navigate(Screen.PAUSE) },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(19.dp)) {
                    val bar = size.width * 0.27f
                    drawRoundRect(Gold, Offset(size.width * .14f, 0f), Size(bar, size.height), CornerRadius(bar / 2))
                    drawRoundRect(Gold, Offset(size.width * .59f, 0f), Size(bar, size.height), CornerRadius(bar / 2))
                }
            }
        }
    }
}

@Composable
private fun Heart(full: Boolean) {
    val fill by animateFloatAsState(if (full) 1f else 0f, tween(250), label = "heart")
    Canvas(Modifier.size(22.dp)) {
        val heart = Path().apply {
            moveTo(size.width * .5f, size.height * .88f)
            cubicTo(-size.width * .14f, size.height * .45f, size.width * .04f, -size.height * .03f,
                size.width * .5f, size.height * .24f)
            cubicTo(size.width * .96f, -size.height * .03f, size.width * 1.14f, size.height * .45f,
                size.width * .5f, size.height * .88f)
            close()
        }
        val squash = .82f + fill * .18f
        withTransform({ scale(squash, squash) }) {
            drawPath(heart, Color(0xff65836b).copy(alpha = .7f), style = Stroke(1.3.dp.toPx()))
            if (fill > 0f) {
                drawPath(heart, Brush.verticalGradient(listOf(Color(0xffffa491), Color(0xffff424e), Color(0xffb81e3c))), fill)
                drawCircle(Color.White.copy(alpha = .76f * fill), 1.4.dp.toPx(),
                    Offset(size.width * .24f, size.height * .28f))
            }
        }
    }
}

@Composable
private fun Overlay(title: String, content: @Composable ColumnScope.() -> Unit) {
    val entrance = rememberEntrance()
    Box(
        Modifier.fillMaxSize().background(Color(0xff081d15).copy(alpha = .59f)).safeDrawingPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 354.dp).fillMaxWidth()
                .graphicsLayer {
                    alpha = entrance.value
                    scaleX = .94f + entrance.value * .06f
                    scaleY = scaleX
                }
                .shadow(16.dp, CardShape).clip(CardShape)
                .background(Brush.verticalGradient(listOf(Color(0xff28543a), DarkMoss)))
                .border(1.dp, Gold.copy(alpha = .56f), CardShape)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 23.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, color = Cream, fontFamily = GameFont, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, fontSize = 32.sp)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun RecordNumber(value: Long) {
    Text(value.toString(), color = Gold, fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Black)
}

@Composable
private fun ScorePair(current: Long, best: Long) {
    Row(
        Modifier.fillMaxWidth().clip(ButtonShape).background(Color(0xff112e21)).padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FINAL SCORE", fontSize = 11.sp, color = SoftGreen)
            Text(current.toString(), fontSize = 30.sp, color = Cream, fontWeight = FontWeight.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BEST SCORE", fontSize = 11.sp, color = SoftGreen)
            Text(best.toString(), fontSize = 30.sp, color = Gold, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun GameButton(label: String, primary: Boolean = false, compact: Boolean = false, action: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, spring(stiffness = 680f), label = "button")
    val colors = if (primary) listOf(Color(0xffffedab), Color(0xffffbf45), Color(0xffee9826))
        else listOf(Color(0xff4d7741), Color(0xff276044), Color(0xff193c2b))
    Box(
        Modifier.fillMaxWidth().heightIn(min = if (compact) 48.dp else 52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, ButtonShape).clip(ButtonShape)
            .background(Brush.verticalGradient(colors))
            .border(1.dp, if (primary) Cream else Gold.copy(alpha = .54f), ButtonShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = action)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), color = if (primary) Color(0xff4f290d) else Cream, fontSize = 15.sp,
            letterSpacing = 1.3.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SoundSwitch(label: String, enabled: Boolean, onChange: () -> Unit) {
    val position by animateFloatAsState(if (enabled) 1f else 0f, tween(170), label = "switch")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 55.dp).clip(ButtonShape).background(DarkMoss)
            .toggleable(value = enabled, role = Role.Switch) { onChange() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(if (enabled) "ON" else "OFF", color = SoftGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.size(43.dp, 23.dp)) {
            drawRoundRect(if (enabled) Color(0xff558c44) else Color(0xff405951),
                cornerRadius = CornerRadius(size.height / 2))
            val radius = size.height * .36f
            val margin = size.height / 2
            drawCircle(if (enabled) Gold else Cream, radius,
                Offset(margin + (size.width - 2 * margin) * position, margin))
        }
    }
}

@Composable
private fun rememberEntrance(): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(360)) }
    return entrance
}
