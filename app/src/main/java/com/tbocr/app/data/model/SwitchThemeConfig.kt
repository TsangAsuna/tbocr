package com.tbocr.app.data.model

import com.tbocr.app.R
import com.tbocr.app.maa.task.MaaTaskParams
import com.tbocr.app.maa.task.MaaTaskType
import com.tbocr.app.utils.i18n.UiText
import com.tbocr.app.utils.i18n.uiTextOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class SwitchThemeConfig(
    val themes: List<String> = emptyList(),
) : TaskParamProvider {
    override fun toTaskParams(ctx: TaskParamContext): List<MaaTaskParams> {
        val candidates = themes.map(String::trim).filter(String::isNotEmpty)
        if (candidates.isEmpty()) {
            ctx.appendLog(uiTextOf(R.string.maa_switch_theme_skipped))
            return emptyList()
        }
        val params = buildJsonObject {
            put("themes", JsonArray(candidates.map(::JsonPrimitive)))
        }
        return listOf(
            MaaTaskParams(
                MaaTaskType.SWITCH_THEME,
                params.toString(),
                logName = UiText.Dynamic(ctx.node.name),
            )
        )
    }
}
