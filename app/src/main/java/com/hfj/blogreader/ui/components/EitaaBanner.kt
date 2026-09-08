package com.hfj.blogreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfj.blogreader.data.models.EitaaPost
import com.hfj.blogreader.ui.theme.LocalFontScale

@Composable
fun EitaaBanner(
    eitaaPost: EitaaPost?,
    onLinkClick: (String) -> Unit
) {
    // اگر پستی وجود نداشت یا متن خالی بود، هیچ‌چیزی نمایش نده
    if (eitaaPost == null || eitaaPost.text.isBlank()) return

    val fontScale = LocalFontScale.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable {
                if (!eitaaPost.link.isNullOrEmpty()) {
                    onLinkClick(eitaaPost.link)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = eitaaPost.text,
            fontSize = 14.sp * fontScale,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}
