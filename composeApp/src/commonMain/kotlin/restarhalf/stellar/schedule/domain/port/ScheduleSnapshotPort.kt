package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.ScheduleSnapshot

/**
 * 课表只读快照端口。
 *
 * 面向「需要一次性读完课表、不参与订阅」的场景（桌面小组件、通知文案等）。
 * 实现方负责把仓库与设置拼成一份自洽的快照。
 */
interface ScheduleSnapshotPort {

    suspend fun load(): ScheduleSnapshot
}
