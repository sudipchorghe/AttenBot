package com.example.attenbot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.attenbot.ui.theme.AppleTheme
import com.example.attenbot.ui.theme.GroupedContainerShape

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(GroupedContainerShape),
        color = AppleTheme.colors.groupedBackground,
        shape = GroupedContainerShape,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
