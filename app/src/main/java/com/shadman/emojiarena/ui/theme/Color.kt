package com.shadman.emojiarena.ui.theme

import androidx.compose.ui.graphics.Color

// Fixed dark palette. No Material You, no light variant — this app only
// ever looks like this.
val BackgroundDark = Color(0xFF0F1012)
val SurfaceDark = Color(0xFF1A1B1F)
val SurfaceVariantDark = Color(0xFF232429)
val OutlineDark = Color(0xFF2C2D33)

val TextPrimary = Color(0xFFF2F2F3)
val TextSecondary = Color(0xFF9A9AA2)

// The one deliberate accent color — used for the user's own bubbles, the
// delivered tick, and the top app bar, so it reads as a considered choice
// rather than a stock template default.
val AccentCoral = Color(0xFFFF6B4A)

val TickSent = TextSecondary
val TickDelivered = AccentCoral

// Ticks inside the user's own (accent-colored) bubble need to contrast
// against that same accent color, so they use white variants instead of
// the tones above.
val TickSentOnAccent = Color.White.copy(alpha = 0.7f)
val TickDeliveredOnAccent = Color.White

// Avatar circle colors, cycled by contact id so initials aren't all the
// same flat color.
val AvatarPalette = listOf(
    Color(0xFFFF6B4A),
    Color(0xFF4FB4FF),
    Color(0xFF9B7BFF),
    Color(0xFF52D68A),
    Color(0xFFFFC15E),
    Color(0xFFFF6FA8),
    Color(0xFF54D1C9)
)
