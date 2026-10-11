package com.example.attenbot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attenbot.ui.theme.AppleTheme
import java.util.Locale

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(Locale.ROOT),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = AppleTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            )
        }

        content()

        if (!footer.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = footer,
                fontSize = 13.sp,
                color = AppleTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )
        }
    }
}
