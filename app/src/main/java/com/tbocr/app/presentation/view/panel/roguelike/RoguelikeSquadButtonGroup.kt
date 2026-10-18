package com.tbocr.app.presentation.view.panel.roguelike

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tbocr.app.domain.enums.RoguelikeMode
import com.tbocr.app.presentation.components.SelectableChipGroup

/**
 * 分队按钮组
 * 根据主题动态生成分队选项
 */
@Composable
fun RoguelikeSquadButtonGroup(
    label: String,
    selectedValue: String,
    theme: String,
    mode: RoguelikeMode,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = localizedRoguelikeSquadOptions(theme, mode)
    val rawValue = selectedValue.ifEmpty { options.firstOrNull()?.first.orEmpty() }

    SelectableChipGroup(
        label = label,
        selectedValue = rawValue,
        options = options,
        onSelected = onValueChange,
        modifier = modifier,
        labelFontWeight = FontWeight.Medium
    )
}
