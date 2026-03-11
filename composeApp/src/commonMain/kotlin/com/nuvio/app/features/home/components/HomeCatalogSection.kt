package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview

@Composable
fun HomeCatalogRowSection(
    section: HomeCatalogSection,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
) {
    NuvioShelfSection(
        title = section.title,
        entries = section.items,
        modifier = modifier,
        onViewAllClick = onViewAllClick,
        key = { item -> item.id },
    ) { item ->
        HomePosterCard(
            item = item,
            onClick = onPosterClick?.let { { it(item) } },
        )
    }
}
