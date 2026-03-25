package restarhalf.stellar.schedule.data.local

import androidx.room.Room
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

fun buildPlatformAppDatabase(): AppDatabase {
    val databasePath = documentDirectoryPath() + "/" + AppDatabase.DATABASE_NAME
    return buildAppDatabase(
        Room.databaseBuilder<AppDatabase>(
            name = databasePath,
            factory = AppDatabaseConstructor::initialize,
        )
    )
}

private fun documentDirectoryPath(): String {
    val directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String
    return directory ?: error("Unable to resolve iOS documents directory")
}