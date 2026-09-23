package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// SCI-FI HUD CYAN & DARK TEAL PALETTE
// =========================================================================

// High-tech Glowing Cyan & Light-Blue Accents (HUD Neon & Rings)
val JarvisCyanGlow = Color(0xFF00F0FF)       // Vibrant electric neon cyan glow
val JarvisCyanBright = Color(0xFF38BDF8)     // Sky bright light-blue highlight
val JarvisCyan = Color(0xFF00E5FF)           // Primary vibrant HUD cyan
val JarvisCyanDark = Color(0xFF0284C7)       // Deep ocean cyan
val JarvisCyanDeep = Color(0xFF0369A1)       // Dark teal cyan panel accent
val JarvisTealAccent = Color(0xFF14B8A6)     // Sci-fi teal highlight
val JarvisNeonTeal = Color(0xFF06B6D4)       // Neon teal accent

// Deep Dark Teal/Cyan Backgrounds (Deep dark blue-green, almost black)
val JarvisSpaceBlack = Color(0xFF020E14)      // Primary deep dark teal background
val JarvisSurfaceDark = Color(0xFF05151D)     // Deep teal surface
val JarvisSurfaceElevated = Color(0xFF091E28) // Slightly elevated dark teal panel
val JarvisSurfaceCard = Color(0xFF0D2633)     // Dark teal module container
val JarvisSurfaceBorder = Color(0xFF13384B)   // Subtle panel divider

// HUD Borders & Glow Effects
val JarvisBorderCyan = Color(0x6600F0FF)     // 40% opacity glowing cyan border
val JarvisBorderGlow = Color(0x9900F0FF)     // 60% opacity neon cyan glow
val JarvisBorderSubtle = Color(0x1F00E5FF)   // 12% opacity subtle technical grid line

// Status & Indicators
val JarvisOrange = Color(0xFFFF9500)
val JarvisGold = Color(0xFFFFCC00)
val JarvisRed = Color(0xFFEF4444)
val JarvisGreen = Color(0xFF10B981)

// Clean High-Contrast Text Colors (Readability against Dark Teal)
val JarvisTextPrimary = Color(0xFFF0FDFF)     // Crisp white with faint cyan tint
val JarvisTextSecondary = Color(0xFFBAE6FD)   // Soft readable light cyan
val JarvisTextMuted = Color(0xFF64748B)       // Muted slate-teal
val JarvisTextGlow = Color(0xFFE0F2FE)        // Luminous light cyan text

// Theme Aliases (Guarantees all screens map to the Glowing Cyan/Teal HUD)
val JarvisGreenGlow = JarvisCyanGlow
val JarvisGreenBright = JarvisCyanBright
val JarvisGreenDark = JarvisCyanDark
val JarvisGreenDeep = JarvisCyanDeep
val JarvisGreenMuted = JarvisTextMuted
val JarvisBorderGreen = JarvisBorderCyan
val JarvisBlue = JarvisCyan
val JarvisBlueDark = JarvisCyanDeep
