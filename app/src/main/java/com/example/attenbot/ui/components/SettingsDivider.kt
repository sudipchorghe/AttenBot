package com.example.attenbot.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.attenbot.ui.theme.AppleTheme

@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 16.dp,
    endIndent: Dp = 0.dp
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent, end = endIndent),
        thickness = 0.5.dp,
        color = AppleTheme.colors.divider
    )
}
