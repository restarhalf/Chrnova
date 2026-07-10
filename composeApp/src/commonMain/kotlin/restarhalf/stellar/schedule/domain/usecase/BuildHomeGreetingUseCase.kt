package restarhalf.stellar.schedule.domain.usecase

class BuildHomeGreetingUseCase {

    fun candidates(courseCount: Int, hasFirstClass: Boolean, dayOfWeekCount: Int): List<String> {
        val list =
            (if (courseCount <= 1) {
                listOf(
                    "你别上了，让我上",
                    "这才是我想要的大学生活",
                    "翘课？不，我根本没课",
                    "这就是自由吗……"
                )
            } else if (courseCount >= 3) {
                listOf(
                    "又是课程满满的一天",
                    "大学五彩缤纷的是课表吗",
                )
            } else {
                listOf()
            })
                .toMutableList()
        list += listOf(
            "想吃抹茶芭菲",
            "对舞萌痴无言了",
            "能陪我上一辈子大学吗",
            "笑容在哪里啊",
            "キラキラ☆ドキドキ！",
            "我要不也去跳个高吧",
            "不要忘记上课哦",
            "外卖还是食堂",
            "今天的风儿甚是喧嚣……",
            "今天也是好天气☆",
            "月が綺麗ねと言われたい",
            "星辰之间增添了荣誉",
            "我们都是洞穴的囚徒",
            "小鸟为什么能在天空飞翔",
            "火种燃尽后会怎么样呢"
        )
        if (hasFirstClass) list += listOf(
            "早八如潮水般袭来",
            "早八人，早八魂",
            "已经没什么好害怕的了"
        )
        if (dayOfWeekCount == 4) list += "疯狂星期四v我50"
        return list.toMutableList()
    }

    operator fun invoke(courseCount: Int, hasFirstClass: Boolean, dayOfWeekCount: Int): String {
        val candidates = candidates(
            courseCount = courseCount,
            hasFirstClass = hasFirstClass,
            dayOfWeekCount = dayOfWeekCount
        )
        return candidates.random()
    }
}
