package restarhalf.stellar.schedule

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform