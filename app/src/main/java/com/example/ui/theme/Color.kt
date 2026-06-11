package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Glassmorphism Dark Theme Colors
val CosmicBackground = Color(0xFF000000)        // Pure OLED Black
val CosmicSurface = Color(0xFF121318)           // Sleek Material Dark Card
val CosmicSurfaceVariant = Color(0xFF1C1625)    // High contrast borders with subtle purple-black tint

// Accent and main brand highlights
val TealPrimary = Color(0xFF00E5FF)            // Neon Cyan Accent
val TealSecondary = Color(0xFF00E5FF)
val TealAccent = Color(0xFF00E5FF)

// Semantic priority colors
val ColorUrgent = Color(0xFFFF4B4B)            // Neon Red
val ColorImportant = Color(0xFFFFB830)         // Golden Yellow
val ColorLow = Color(0xFF4BFF91)               // Emerald Green
val ColorSilent = Color(0xFF6B7280)            // Cool Muted Grey

// Backward compatibility or fallback helpers
val GoogleBackground = CosmicBackground
val GoogleSurface = CosmicSurface
val GoogleSurfaceVariant = CosmicSurfaceVariant
val GoogleBlue = TealPrimary
val GoogleRed = ColorUrgent
val GoogleYellow = ColorImportant
val GoogleGreen = ColorLow
val GoogleCyan = TealAccent
