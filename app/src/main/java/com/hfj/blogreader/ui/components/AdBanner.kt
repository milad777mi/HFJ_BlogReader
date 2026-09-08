package com.hfj.blogreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader  // ✅ پشتیبانی از GIF
import com.hfj.blogreader.data.models.AdData
import com.hfj.blogreader.ui.theme.LocalFontScale

@Composable
fun AdBanner(
    adData: AdData?,
    onAdClick: (String) -> Unit
) {
    if (adData == null || adData.imageUrl.isNullOrEmpty()) return

    val fontScale = LocalFontScale.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable {
                if (!adData.link.isNullOrEmpty()) {
                    onAdClick(adData.link)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = adData.imageUrl,
                contentDescription = adData.title ?: "تبلیغات",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentScale = ContentScale.FillBounds,
                imageLoader = LocalImageLoader.current  // ✅ پشتیبانی از GIF
            )
            if (!adData.title.isNullOrEmpty()) {
                Text(
                    text = adData.title,
                    fontSize = 11.sp * fontScale,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
