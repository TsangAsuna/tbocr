package com.tbocr.app.data.model

import com.tbocr.app.R
import com.tbocr.app.data.resource.ServerTimezone
import com.tbocr.app.domain.models.PlanSideTask
import com.tbocr.app.domain.models.UserDataUpdateTriggerInterval
import com.tbocr.app.domain.models.isUserDataUpdateDue
import com.tbocr.app.maa.task.MaaTaskParams
import com.tbocr.app.maa.task.MaaTaskType
import com.tbocr.app.utils.i18n.uiTextOf
import kotlinx.serialization.Serializable

/** 更新数据：按间隔展开为 0~2 个识别任务。 */
@Serializable
data class UserDataUpdateConfig(
    val updateOperBox: Boolean = true,
    val updateDepot: Boolean = true,
    val triggerInterval: UserDataUpdateTriggerInterval = UserDataUpdateTriggerInterval.EVERY_TIME,
) : TaskParamProvider {

    override fun toTaskParams(ctx: TaskParamContext): List<MaaTaskParams> {
        if (!updateOperBox && !updateDepot) {
            return emptyList()
        }

        val yjToday = ServerTimezone.getYjDate(ctx.clientType)
        val yjZone = ServerTimezone.getServerZone(ctx.clientType)
        val operDue = updateOperBox && isUserDataUpdateDue(
            lastSyncMillis = ctx.operBoxRepository.snapshot.value.syncTimeMillis,
            interval = triggerInterval,
            yjToday = yjToday,
            yjZone = yjZone,
        )
        val depotDue = updateDepot && isUserDataUpdateDue(
            lastSyncMillis = ctx.depotRepository.snapshot.value.syncTimeMillis,
            interval = triggerInterval,
            yjToday = yjToday,
            yjZone = yjZone,
        )
        if (!operDue && !depotDue) {
            return emptyList()
        }

        // 干员识别改走一图流：不占任务位，启动时并行拉取
        val operViaYituliu = operDue && ctx.operBoxUseYituliuApi
        if (operViaYituliu) {
            ctx.registerSideTask(PlanSideTask.OPER_BOX_YITULIU)
        }

        // 对齐上游：先干员后仓库（串行）。
        return buildList {
            if (operDue && !operViaYituliu) add(
                MaaTaskParams(
                    MaaTaskType.OPER_BOX,
                    "{}",
                    logName = uiTextOf(R.string.runlog_task_with_detail, ctx.node.name, uiTextOf(R.string.maa_oper_box)),
                )
            )
            if (depotDue) add(
                MaaTaskParams(
                    MaaTaskType.DEPOT,
                    "{}",
                    logName = uiTextOf(R.string.runlog_task_with_detail, ctx.node.name, uiTextOf(R.string.maa_depot)),
                )
            )
        }
    }
}
