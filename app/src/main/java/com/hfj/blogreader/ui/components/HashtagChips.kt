package com.hfj.blogreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfj.blogreader.ui.theme.LocalFontScale

@Composable
fun HashtagChips(
    hashtags: List<String>,
    onTagTap: (String) -> Unit
) {
    val fontScale = LocalFontScale.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        hashtags.take(5).forEach { tag ->
            AssistChip(
                onClick = { onTagTap(tag) },
                label = {
                    Text(
                        tag,
                        fontSize = 11.sp * fontScale,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
