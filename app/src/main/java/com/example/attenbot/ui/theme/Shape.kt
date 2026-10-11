package com.example.attenbot.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GroupedContainerShape = RoundedCornerShape(14.dp)
val ButtonShape = RoundedCornerShape(10.dp)
val PillShape = CircleShape
val IconBoxShape = RoundedCornerShape(7.dp)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = ButtonShape,           // 10.dp buttons
    large = GroupedContainerShape,  // 14.dp grouped container
    extraLarge = PillShape          // Pill
)
