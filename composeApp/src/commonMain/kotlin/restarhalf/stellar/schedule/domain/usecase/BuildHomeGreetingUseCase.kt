package restarhalf.stellar.schedule.domain.usecase

class BuildHomeGreetingUseCase {

    fun candidates(courseCount: Int, hasFirstClass: Boolean): List<String> {
        val list =
            (if (courseCount <= 1) {
                listOf("你别上了，让我上", "这才是我想要的大学生活")
            } else if (courseCount >= 3) {
                listOf("又是课程满满的一天", "大学五彩缤纷的是课表吗")
            } else {
                listOf("不要忘记上课哦", "外卖还是食堂")
            })
                .toMutableList()
        list += "想吃抹茶芭菲"
        if (hasFirstClass) list += "早八如潮水般袭来"
        return list
    }

    operator fun invoke(courseCount: Int, hasFirstClass: Boolean, seed: Int): String {
        val candidates = candidates(courseCount = courseCount, hasFirstClass = hasFirstClass)
        if (candidates.isEmpty()) return "今天也要加油"
        val index = seed.mod(candidates.size)
        return candidates[index]
    }
}
