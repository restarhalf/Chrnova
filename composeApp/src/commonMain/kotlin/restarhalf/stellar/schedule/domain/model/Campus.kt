package restarhalf.stellar.schedule.domain.model

/**
 * 校区枚举类
 * 
 * 定义应用支持的校区类型，不同校区可能有不同的课程安排和教务系统配置。
 */
enum class Campus {
    /** 开发测试校区 */
    Development,
    /** 金石滩校区（默认） */
    Jinshitan,
}
