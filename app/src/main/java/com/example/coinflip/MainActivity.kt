package com.example.coinflip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoinFlipTheme {
                Surface(color = Color(0xFF07100C)) {
                    CoinFlipApp()
                }
            }
        }
    }
}

// ---------- Theme & palette ----------

private val Gold = Color(0xFFD4AF37)
private val GoldMuted = Color(0xFFC7A250)
private val Cream = Color(0xFFFBF3DE)
private val RimDark = Color(0xFF6B5017)
private val RimLight = Color(0xFFF3DFA0)
private val EdgeShade = Color(0xFF7C5D1E)

@Composable
fun CoinFlipTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Gold,
        background = Color(0xFF07100C),
        surface = Color(0xFF07100C)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

private fun coinGradient(): Brush = Brush.radialGradient(
    colors = listOf(Color(0xFFF7E6AC), Color(0xFFDBB752), Color(0xFF9C7A24))
)

private fun tableGradient(): Brush = Brush.radialGradient(
    colors = listOf(Color(0xFF122A1E), Color(0xFF0A1712), Color(0xFF07100C))
)

// ---------- State ----------

private enum class Face { FRONT, BACK }

private class CoinState(val number: Int) {
    val rotation = Animatable(0f)
    var face by mutableStateOf(Face.FRONT)
    var isFlipping by mutableStateOf(false)
    var hasLanded by mutableStateOf(false)
}

// ---------- App ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipApp() {
    var coinCount by remember { mutableIntStateOf(1) }
    val coinStates = remember(coinCount) { List(coinCount) { CoinState(it + 1) } }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    fun flipAll() {
        if (coinStates.any { it.isFlipping }) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        coinStates.forEach { coin ->
            scope.launch {
                coin.isFlipping = true
                coin.hasLanded = false
                val landsOnBack = Random.nextBoolean()
                val extraTurns = Random.nextInt(4, 7)
                val currentFullTurns = (coin.rotation.value / 360f).toInt()
                val base = (currentFullTurns + extraTurns) * 360f
                val target = if (landsOnBack) base + 180f else base + 360f
                coin.rotation.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 900 + Random.nextInt(0, 300),
                        easing = CubicBezierEasing(0.22f, 0.08f, 0.24f, 1f)
                    )
                ) {
                    val mod = ((value % 360f) + 360f) % 360f
                    coin.face = if (mod in 90f..270f) Face.BACK else Face.FRONT
                }
                coin.isFlipping = false
                coin.hasLanded = true
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Coin Flip",
                        color = Cream,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    CoinCountSelector(
                        current = coinCount,
                        expanded = menuExpanded,
                        onExpandChange = { menuExpanded = it },
                        onSelect = {
                            coinCount = it
                            menuExpanded = false
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tableGradient())
                .padding(padding)
        ) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                CoinsLayout(coinStates = coinStates, onTap = { flipAll() })
            }
            Text(
                text = "TAP A COIN TO FLIP",
                color = GoldMuted.copy(alpha = 0.5f),
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

// ---------- Top-right coin count selector ----------

@Composable
private fun CoinCountSelector(
    current: Int,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1FFFFFFF))
                .border(1.dp, GoldMuted.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onExpandChange(true) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "$current", color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (current == 1) "coin" else "coins",
                color = Cream.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Change number of coins",
                tint = GoldMuted
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.background(Color(0xFF10241A))
        ) {
            (1..4).forEach { n ->
                DropdownMenuItem(
                    text = { Text(text = "$n ${if (n == 1) "Coin" else "Coins"}", color = Cream) },
                    onClick = { onSelect(n) }
                )
            }
        }
    }
}

// ---------- Coin layout for 1-4 coins ----------

@Composable
private fun CoinsLayout(coinStates: List<CoinState>, onTap: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        val spacing = 20.dp
        when (coinStates.size) {
            1 -> {
                val coinSize = minOf(maxWidth * 0.6f, 230.dp)
                CoinView(coinStates[0], coinSize, onTap)
            }
            2 -> {
                val coinSize = minOf((maxWidth - spacing) / 2, 190.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    coinStates.forEach { CoinView(it, coinSize, onTap) }
                }
            }
            3 -> {
                val coinSize = minOf((maxWidth - spacing * 2) / 3, 150.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    coinStates.forEach { CoinView(it, coinSize, onTap) }
                }
            }
            else -> {
                val coinSize = minOf((maxWidth - spacing) / 2, 160.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        CoinView(coinStates[0], coinSize, onTap)
                        CoinView(coinStates[1], coinSize, onTap)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        CoinView(coinStates[2], coinSize, onTap)
                        CoinView(coinStates[3], coinSize, onTap)
                    }
                }
            }
        }
    }
}

// ---------- Single coin: tilt, flip animation, edge illusion ----------

@Composable
private fun CoinView(state: CoinState, size: Dp, onTap: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationX = -7f
                    rotationY = state.rotation.value
                    cameraDistance = 28.dp.toPx()
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onTap() },
            contentAlignment = Alignment.Center
        ) {
            // subtle rim peeking underneath to fake cylindrical thickness
            Box(
                modifier = Modifier
                    .fillMaxSize(0.97f)
                    .offset(y = size * 0.045f)
                    .clip(CircleShape)
                    .background(EdgeShade)
            )
            if (state.face == Face.FRONT) {
                CoinFace(number = state.number, coinSize = size)
            } else {
                CoinBackFace(coinSize = size)
            }
        }
        AnimatedVisibility(
            visible = state.hasLanded && !state.isFlipping,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = if (state.face == Face.FRONT) "HEADS" else "TAILS",
                color = GoldMuted,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun CoinFace(number: Int, coinSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.6f))
            .clip(CircleShape)
            .background(coinGradient())
            .border(width = coinSize * 0.035f, color = RimDark, shape = CircleShape)
            .padding(coinSize * 0.09f)
            .border(width = coinSize * 0.015f, color = RimLight.copy(alpha = 0.65f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = (coinSize.value * 0.4f).sp,
            color = Cream
        )
    }
}

@Composable
private fun CoinBackFace(coinSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.6f))
            .clip(CircleShape)
            .background(coinGradient())
            .border(width = coinSize * 0.035f, color = RimDark, shape = CircleShape)
            .padding(coinSize * 0.09f)
            .border(width = coinSize * 0.015f, color = RimLight.copy(alpha = 0.65f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(coinSize * 0.24f)
                .graphicsLayer { rotationZ = 45f }
                .border(width = coinSize * 0.02f, color = Cream.copy(alpha = 0.85f))
        )
    }
}
