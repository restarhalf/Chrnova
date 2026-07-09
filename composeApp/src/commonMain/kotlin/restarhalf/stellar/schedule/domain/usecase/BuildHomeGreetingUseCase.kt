package restarhalf.stellar.schedule.domain.usecase

class BuildHomeGreetingUseCase {

    fun candidates(courseCount: Int, hasFirstClass: Boolean, dayOfWeekCount: Int): List<String> {
        val list =
            (if (courseCount <= 1) {
                listOf("你别上了，让我上", "这才是我想要的大学生活")
            } else if (courseCount >= 3) {
                listOf("又是课程满满的一天", "大学五彩缤纷的是课表吗")
            } else {
                listOf("不要忘记上课哦", "外卖还是食堂")
            })
                .toMutableList()
        list += listOf(
            "想吃抹茶芭菲",
            "对舞萌痴无言了",
            "能陪我上一辈子大学吗",
            "笑容在哪里啊",
            "キラキラ☆ドキドキ！"
        )
        if (hasFirstClass) list += "早八如潮水般袭来"
        if (dayOfWeekCount == 4) list += "疯狂星期四v我50"
        return list.toMutableList()
    }

    operator fun invoke(courseCount: Int, hasFirstClass: Boolean, dayOfWeekCount: Int): String {
        val candidates = candidates(
            courseCount = courseCount,
            hasFirstClass = hasFirstClass,
            dayOfWeekCount = dayOfWeekCount
        )
        if (candidates.isEmpty()) return "今天也要加油"
        return candidates.random()
    }
}
