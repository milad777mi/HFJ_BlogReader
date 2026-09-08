package com.hfj.blogreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hfj.blogreader.data.models.AdData
import com.hfj.blogreader.data.models.EitaaPost
import com.hfj.blogreader.ui.theme.LocalFontScale

@Composable
fun AdBanner(
    adData: AdData?,
    onAdClick: (String) -> Unit
) {
    if (adData == null || !adData.exists || adData.imageUrl == null) return

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
                    .height(50.dp)
                    .padding(0.dp),
                contentScale = ContentScale.FillBounds
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

@Composable
fun EitaaBanner(
    eitaaPost: EitaaPost?,
    onLinkClick: (String) -> Unit
) {
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
