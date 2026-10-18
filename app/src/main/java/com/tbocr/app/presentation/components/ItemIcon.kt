package com.tbocr.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tbocr.app.data.resource.ItemIconLoader
import org.koin.compose.koinInject

/** 物品图标，对应 WPF ItemImageConverter */
@Composable
fun ItemIcon(
    itemId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    loader: ItemIconLoader = koinInject(),
) {
    TemplateImage(
        id = itemId,
        loader = loader,
        modifier = modifier,
        contentDescription = contentDescription,
    )
}
