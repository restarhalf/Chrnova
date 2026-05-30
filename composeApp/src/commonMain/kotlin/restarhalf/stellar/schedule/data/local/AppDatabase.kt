package restarhalf.stellar.schedule.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import restarhalf.stellar.schedule.platform.AppIoDispatcher

@Database(
    entities = [
        Course::class,
        ExaminationEntity::class,
        GradeEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun examinationDao(): ExaminationDao
    abstract fun gradeDao(): GradeDao

    companion object {
        const val DATABASE_NAME: String = "schedule.db"
    }
}

@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

private val migration2To3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration3To4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN remoteKey TEXT NOT NULL DEFAULT ''")
    }
}

private val migration4To5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN originRemoteKey TEXT")
    }
}

private val migration5To6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN targetWeek INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration6To7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN semesterId TEXT NOT NULL DEFAULT ''")
    }
}

private val migration7To8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `examinations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseNumber` TEXT NOT NULL, `courseName` TEXT NOT NULL, `time` TEXT NOT NULL, `examinationPlace` TEXT NOT NULL, `zwh` TEXT NOT NULL, `ksbz` TEXT NOT NULL, `semesterId` TEXT NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `grades` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseCode` TEXT NOT NULL, `courseName` TEXT NOT NULL, `score` TEXT NOT NULL, `gradePoint` REAL NOT NULL, `credit` REAL NOT NULL, `curriculumAttributes` TEXT NOT NULL, `courseNature` TEXT NOT NULL, `examName` TEXT NOT NULL, `examinationNature` TEXT NOT NULL, `passStatus` TEXT NOT NULL, `gradeLevel` TEXT NOT NULL, `markFlag` TEXT NOT NULL, `repeatSemester` TEXT NOT NULL, `gradeId` TEXT NOT NULL, `semester` TEXT NOT NULL)"
        )
    }
}

fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(AppIoDispatcher)
        .fallbackToDestructiveMigration(false)
        .addMigrations(
            migration2To3,
            migration3To4,
            migration4To5,
            migration5To6,
            migration6To7,
            migration7To8
        )
        .build()
