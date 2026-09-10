package com.hfj.blogreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfj.blogreader.data.models.AdTextItem
import com.hfj.blogreader.ui.theme.LocalFontScale

@Composable
fun AdTextBanner(
    adTextItem: AdTextItem?,
    onLinkClick: (String) -> Unit
) {
    // اگر پیامی وجود نداشت یا متن خالی بود، هیچ‌چیزی نمایش نده
    if (adTextItem == null || adTextItem.text.isBlank()) return

    val fontScale = LocalFontScale.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable {
                if (!adTextItem.link.isNullOrEmpty()) {
                    onLinkClick(adTextItem.link)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = adTextItem.text,
            fontSize = 14.sp * fontScale,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}
