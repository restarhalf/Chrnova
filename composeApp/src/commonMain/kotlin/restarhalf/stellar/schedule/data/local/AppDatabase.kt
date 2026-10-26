package restarhalf.stellar.schedule.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import restarhalf.stellar.schedule.data.local.dao.CourseDao
import restarhalf.stellar.schedule.data.local.dao.ExaminationDao
import restarhalf.stellar.schedule.data.local.dao.GradeDao
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.local.entity.CourseEntity
import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity
import restarhalf.stellar.schedule.data.local.entity.GradeEntity
import restarhalf.stellar.schedule.data.local.entity.PEDetailSummaryEntity
import restarhalf.stellar.schedule.data.local.entity.PESubjectScoreEntity
import restarhalf.stellar.schedule.data.local.entity.PEYearScoreEntity
import restarhalf.stellar.schedule.platform.AppIoDispatcher

@Database(
    entities = [
        CourseEntity::class,
        ExaminationEntity::class,
        GradeEntity::class,
        PEYearScoreEntity::class,
        PESubjectScoreEntity::class,
        PEDetailSummaryEntity::class
    ],
    version = 20,
    exportSchema = true
)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    
    abstract fun examinationDao(): ExaminationDao
    
    abstract fun gradeDao(): GradeDao
    
    abstract fun peYearScoreDao(): PEYearScoreDao
    
    abstract fun peDetailDao(): PEDetailDao

    companion object {
        const val DATABASE_NAME: String = "schedule.db"
    }
}

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

private val migration8To9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_scores` (`schoolYear` TEXT PRIMARY KEY NOT NULL, `total` REAL NOT NULL, `isFree` INTEGER NOT NULL, `done` INTEGER NOT NULL, `nums` INTEGER NOT NULL)"
        )
    }
}

private val migration9To10 = object : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_student_info` (`id` TEXT PRIMARY KEY NOT NULL, `testCode` TEXT NOT NULL, `stuName` TEXT NOT NULL, `stdNumber` TEXT NOT NULL)"
        )
    }
}

private val migration10To11 = object : Migration(10, 11) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_detail_scores` (`schoolYear` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `subName` TEXT NOT NULL, `result` TEXT, `score` INTEGER, `unit` TEXT NOT NULL, `subRatio` TEXT NOT NULL, `grade` TEXT, `isJoin` INTEGER NOT NULL, PRIMARY KEY (`schoolYear`, `subjectId`))"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_detail_summary` (`schoolYear` TEXT PRIMARY KEY NOT NULL, `totalScore` REAL NOT NULL, `totalGrade` TEXT NOT NULL)"
        )
    }
}

private val migration11To12 = object : Migration(11, 12) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE examinations ADD COLUMN source TEXT NOT NULL DEFAULT 'sync'")
    }
}

private val migration12To13 = object : Migration(12, 13) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE examinations ADD COLUMN userNo TEXT NOT NULL DEFAULT ''")
    }
}

private val migration13To14 = object : Migration(13, 14) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN userNo TEXT NOT NULL DEFAULT ''")
    }
}

private val migration14To15 = object : Migration(14, 15) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_courses_semesterId ON courses(semesterId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_courses_userNo ON courses(userNo)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_examinations_semesterId ON examinations(semesterId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_examinations_userNo ON examinations(userNo)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_grades_semester ON grades(semester)")
    }
}

private val migration15To16 = object : Migration(15, 16) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE grades ADD COLUMN userNo TEXT NOT NULL DEFAULT ''")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_grades_userNo ON grades(userNo)")
    }
}

private val migration16To17 = object : Migration(16, 17) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_courses_userNo_semesterId ON courses(userNo, semesterId)")
    }
}

private val migration17To18 = object : Migration(17, 18) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_courses_type ON courses(type)")
    }
}

private val migration18To19 = object : Migration(18, 19) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_grades_userNo_semester ON grades(userNo, semester)")
    }
}

private val migration19To20 = object : Migration(19, 20) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS pe_student_info")
    }
}

fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(AppIoDispatcher)
        .fallbackToDestructiveMigrationOnDowngrade()
        .addMigrations(
            migration2To3,
            migration3To4,
            migration4To5,
            migration5To6,
            migration6To7,
            migration7To8,
            migration8To9,
            migration9To10,
            migration10To11,
            migration11To12,
            migration12To13,
            migration13To14,
            migration14To15,
            migration15To16,
            migration16To17,
            migration17To18,
            migration18To19,
            migration19To20
        )
        .build()
